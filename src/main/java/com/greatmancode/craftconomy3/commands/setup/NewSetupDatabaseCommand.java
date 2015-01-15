/*
 * This file is part of Craftconomy3.
 *
 * Copyright (c) 2011-2014, Greatman <http://github.com/greatman/>
 *
 * Craftconomy3 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Craftconomy3 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Craftconomy3.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.greatmancode.craftconomy3.commands.setup;

import com.greatmancode.craftconomy3.Common;
import com.greatmancode.craftconomy3.NewSetupWizard;
import com.greatmancode.tools.commands.interfaces.CommandExecutor;
import com.greatmancode.tools.utils.Tools;

import java.util.HashMap;
import java.util.Map;

public class NewSetupDatabaseCommand extends CommandExecutor {
    private enum INTERNALSTEP {
        START,
        SQLITE,
        MYSQL,
        H2;
    }

    private static final Map<String, String> VALUES = new HashMap<String, String>();
    private static final String ERROR_MESSAGE = "{{DARK_RED}}오류가 발생했습니다. 오류는: {{WHITE}}%s";
    private static final String CONFIG_NODE = "System.Database.Type";
    private INTERNALSTEP step = INTERNALSTEP.START;

    @Override
    public void execute(String sender, String[] args) {
        if (NewSetupWizard.getState().equals(NewSetupWizard.DATABASE_STEP)) {
            if (step.equals(INTERNALSTEP.START)) {
                start(sender, args);
            } else if (step.equals(INTERNALSTEP.MYSQL)) {
                mysql(sender, args);
            }
        }
    }

    @Override
    public String help() {
        return "/경제설치 데이터베이스 - 데이터베이스 단계 설치 마법사.";
    }

    @Override
    public int maxArgs() {
        return 3;
    }

    @Override
    public int minArgs() {
        return 0;
    }

    @Override
    public boolean playerOnly() {
        return false;
    }

    @Override
    public String getPermissionNode() {
        return "craftconomy.setup";
    }

    private void start(String sender, String[] args) {
        if (args.length == 1) {
            if ("mysql".equalsIgnoreCase(args[0])) {
                step = INTERNALSTEP.MYSQL;
                Common.getInstance().getMainConfig().setValue(CONFIG_NODE, "mysql");
                Common.getInstance().getServerCaller().getPlayerCaller().sendMessage(sender, "{{WHITE}}MySQL{{DARK_GREEN}}을 선택했습니다. {{WHITE}}/경제설치 데이터베이스 주소 <호스트> {{DARK_GREEN}}를 치세요");
            } else if ("h2".equalsIgnoreCase(args[0])) {
                step = INTERNALSTEP.H2;
                h2(sender);
            } else {
                Common.getInstance().getServerCaller().getPlayerCaller().sendMessage(sender, "{{DARK_RED}}올바르지 않은 값!");
                Common.getInstance().getServerCaller().getPlayerCaller().sendMessage(sender, "{{WHITE}}/경제설치 데이터베이스 <mysql/h2> {{DARK_GREEN}}를 치세요");
            }
        }
    }

    private void h2(String sender) {
        Common.getInstance().getMainConfig().setValue(CONFIG_NODE, "h2");
        Common.getInstance().initialiseDatabase();
        done(sender);
    }

    private void mysql(String sender, String[] args) {
        if (args.length == 2) {
            if ("주소".equalsIgnoreCase(args[0])) {
                VALUES.put("address", args[1]);
                Common.getInstance().getMainConfig().setValue("System.Database.Address", args[1]);
                Common.getInstance().getServerCaller().getPlayerCaller().sendMessage(sender, "{{DARK_GREEN}}모두 좋습니다! {{WHITE}}/경제설치 데이터베이스 포트 <포트> {{DARK_GREEN}}로 MySQL 포트를 입력하세요 (보통 3306입니다)");
            } else if ("포트".equalsIgnoreCase(args[0])) {
                if (Tools.isInteger(args[1])) {
                    int port = Integer.parseInt(args[1]);
                    VALUES.put("port", args[1]);
                    Common.getInstance().getMainConfig().setValue("System.Database.Port", port);
                    Common.getInstance().getServerCaller().getPlayerCaller().sendMessage(sender, "{{DARK_GREEN}}저장됨! {{WHITE}}/경제설치 데이터베이스 사용자이름 <사용자이름> {{DARK_GREEN}}으로  MySQL 사용자 이름을 입력하세요");
                } else {
                    Common.getInstance().getServerCaller().getPlayerCaller().sendMessage(sender, "{{DARK_RED}}올바르지 않은 포트!");
                }
            } else if ("사용자이름".equalsIgnoreCase(args[0])) {
                VALUES.put("username", args[1]);
                Common.getInstance().getMainConfig().setValue("System.Database.Username", args[1]);
                Common.getInstance().getServerCaller().getPlayerCaller().sendMessage(sender, "{{DARK_GREEN}}저장됨! {{WHITE}}/경제설치 데이터베이스 비밀번호 <비밀번호> {{DARK_GREEN}}로 MySQL 비밀번호를 입력하세요 (없으면 \"\" 를 입력하세요)");
            } else if ("비밀번호".equalsIgnoreCase(args[0])) {
                if (args[1].equals("''") || args[1].equals("\"\"")) {
                    VALUES.put("password", "");
                    Common.getInstance().getMainConfig().setValue("System.Database.Password", "");
                } else {
                    VALUES.put("password", args[1]);
                    Common.getInstance().getMainConfig().setValue("System.Database.Password", args[1]);
                }
                Common.getInstance().getServerCaller().getPlayerCaller().sendMessage(sender, "{{DARK_GREEN}}저장됨! {{WHITE}}/경제설치 데이터베이스 db <데이터베이스 이름> {{DARK_GREEN}}으로 MySQL 데이터베이스를 입력하세요.");
            } else if ("db".equalsIgnoreCase(args[0])) {
                VALUES.put("db", args[1]);
                Common.getInstance().getMainConfig().setValue("System.Database.Db", args[1]);
                Common.getInstance().getServerCaller().getPlayerCaller().sendMessage(sender, "{{DARK_GREEN}}저장됨! {{WHITE}}/경제설치 데이터베이스 접두사 <접두사> {{DARK_GREEN}}로 데이블의 접두사를 입력하세요(확실하지 않으면, cc3_ 를 입력하세요).");
            } else if ("접두사".equalsIgnoreCase(args[0])) {
                VALUES.put("prefix", args[1]);
                Common.getInstance().getMainConfig().setValue("System.Database.Prefix", args[1]);
                Common.getInstance().getServerCaller().getPlayerCaller().sendMessage(sender, "{{DARK_GREEN}}완료! 데이터베이스를 초기값으로 설정하는 동안 기다려주세요.");
            }
        }

        if (VALUES.size() == 6) {
            Common.getInstance().initialiseDatabase();
            done(sender);
            //TODO: A catch
        }
    }

    private void done(String sender) {
        Common.getInstance().initializeCurrency();
        Common.getInstance().getServerCaller().getPlayerCaller().sendMessage(sender, "{{DARK_GREEN}}모두 좋습니다! Craftconomy에 오신것을 환영합니다! 우리는 다중-통화 구조를 사용합니다. 기본 통화를 위한 설정 작성이 필요합니다.");
        Common.getInstance().getServerCaller().getPlayerCaller().sendMessage(sender, "{{DARK_GREEN}}맨 먼저, {{WHITE}}주오 통화 이름{{DARK_GREEN}}을 구성합시다 (Ex: {{WHITE}}Dollar{{DARK_GREEN}}). {{WHITE}}/경제설치 통화 이름 <이름> {{DARK_GREEN}}을 쳐주세요");
        NewSetupWizard.setState(NewSetupWizard.CURRENCY_STEP);
    }
}
