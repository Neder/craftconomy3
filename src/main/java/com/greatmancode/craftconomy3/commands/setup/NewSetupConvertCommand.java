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
import com.greatmancode.craftconomy3.converter.Converter;
import com.greatmancode.craftconomy3.converter.ConverterList;
import com.greatmancode.tools.commands.interfaces.CommandExecutor;

import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

public class NewSetupConvertCommand extends CommandExecutor {
    private enum INTERNALSTEP {
        START,
        SELECT_CONVERT,
        SELECT_DB,
        INSERT_VALUES,
        CONVERT;
    }

    private static final ConverterList IMPORTER_LIST = new ConverterList();
    private static Converter selectedConverter = null;
    private INTERNALSTEP step = INTERNALSTEP.START;

    @Override
    public void execute(String sender, String[] args) {
        if (NewSetupWizard.getState().equals(NewSetupWizard.CONVERT_STEP)) {
            if (step.equals(INTERNALSTEP.START)) {
                start(sender, args);
            } else if (step.equals(INTERNALSTEP.SELECT_CONVERT)) {
                selectConvert(sender, args);
            } else if (step.equals(INTERNALSTEP.SELECT_DB)) {
                selectDb(sender, args);
            } else if (step.equals(INTERNALSTEP.INSERT_VALUES)) {
                selectValues(sender, args);
            }
        }
    }

    @Override
    public String help() {
        return "/경제설치 전환 - 전환 마법사.";
    }

    @Override
    public int maxArgs() {
        return 3;
    }

    @Override
    public int minArgs() {
        return 1;
    }

    @Override
    public boolean playerOnly() {
        return false;
    }

    @Override
    public String getPermissionNode() {
        return "craftconomy.setup";
    }

    private void selectValues(final String sender, String[] args) {
        if (args.length <= 2) {
            if (selectedConverter != null) {
                if (selectedConverter.setDbInfo(args[0], args[1])) {
                    if (selectedConverter.allSet()) {
                        //We start the convert!
                        if (selectedConverter.connect()) {
                            Common.getInstance().getServerCaller().getPlayerCaller().sendMessage(sender, "{{DARK_GREEN}}모든 값이 정상입니다! 전환을 시작합니다!");
                            Common.getInstance().getServerCaller().getSchedulerCaller().schedule(new Runnable() {
                                @Override
                                public void run() {
                                    Common.getInstance().getServerCaller().getPlayerCaller().sendMessage(sender, "{{DARK_RED}}주의: {{WHITE}}이 전환은 다른 쓰레드를 만들기 때문에 서버에 걸려있지 않습니다. Craftconomy는 전환이 완료되면 잠금이 해제될 것입니다.");
                                    selectedConverter.importData(sender);
                                    Common.getInstance().getMainConfig().setValue("System.Setup", false);
                                    Common.getInstance().reloadPlugin();

                                    Common.getInstance().getServerCaller().getPlayerCaller().sendMessage(sender, "{{DARK_GREEN}}전환 완료! Craftconomy를 즐기세요!");
                                }
                            }, 0, 0, true);
                        } else {
                            Common.getInstance().getServerCaller().getPlayerCaller().sendMessage(sender, "{{DARK_RED}}몇몇 설정이 잘못되었습니다. 모든 설정이 정상인지 확실히 해주세요! 콘솔 기록을 보시면 더 많은 정보를 볼 수 있습니다.");
                        }
                    } else {
                        Common.getInstance().getServerCaller().getPlayerCaller().sendMessage(sender, "{{DARK_GREEN}}값이 {{WHITE}}" + args[0] + "{{DARK_GREEN}}(으)로 설정 되었습니다. 계속해주세요.");
                    }
                }
            } else {
                Common.getInstance().getServerCaller().getPlayerCaller().sendMessage(sender, "{{DARK_RED}}무언가가 잘못되었습니다. 선택된 전환자가 없습니다!");
            }
        }
    }

    private void selectDb(String sender, String[] args) {
        if (selectedConverter.getDbTypes().contains(args[0])) {
            selectedConverter.setDbType(args[0]);
            step = INTERNALSTEP.INSERT_VALUES;
            if (selectedConverter.getDbInfo().size() == 0) {
                selectedConverter.importData(sender);
                Common.getInstance().getMainConfig().setValue("System.Setup", false);
                Common.getInstance().reloadPlugin();
                Common.getInstance().getServerCaller().getPlayerCaller().sendMessage(sender, "{{DARK_GREEN}}전환 완료! Craftconomy를 즐기세요!");
            } else {
                Common.getInstance().getServerCaller().getPlayerCaller().sendMessage(sender, args[0] + " {{DARK_GREEN}}선택됨. 이제, 데이터베이스 형식을 고를 올바른 값을 입력해주세요. 구문: {{WHITE}}/경제설치 전환 <" + formatListString(selectedConverter.getDbInfo()) + "> <값>");
                Common.getInstance().getServerCaller().getPlayerCaller().sendMessage(sender, "{{DARK_GREEN}}예제: {{WHITE}}/경제설치 전환 " + selectedConverter.getDbInfo().get(0) + " test");
            }
        } else {
            Common.getInstance().getServerCaller().getPlayerCaller().sendMessage(sender, "{{DARK_RED}}이 데이터베이스 종류는 존재하지 않습니다! {{WHITE}}/경제설치 전환 <" + formatListString(selectedConverter.getDbTypes()) + "> {{DARK_RED}}을 쳐주세요");
        }
    }

    private void selectConvert(String sender, String[] args) {
        if (IMPORTER_LIST.getConverterList().containsKey(args[0])) {
            selectedConverter = IMPORTER_LIST.getConverterList().get(args[0]);
            Common.getInstance().getServerCaller().getPlayerCaller().sendMessage(sender, "{{WHITE}}" + args[0] + " {{DARK_GREEN}}수입자 선택됨.");
            if (selectedConverter.getWarning() != null) {
                Common.getInstance().getServerCaller().getPlayerCaller().sendMessage(sender, "{{DARK_RED}}경고{{WHITE}}: " + selectedConverter.getWarning());
            }
            if (selectedConverter.getDbTypes().size() == 1) {
                step = INTERNALSTEP.SELECT_DB;
                selectDb(sender, new String[]{selectedConverter.getDbTypes().get(0)});
            } else {
                Common.getInstance().getServerCaller().getPlayerCaller().sendMessage(sender, "{{DARK_GREEN}}이 전환자는 아래의 데이터베이스 종류들을 지원합니다. 하나를 선택해주세요");
                Common.getInstance().getServerCaller().getPlayerCaller().sendMessage(sender, "{{WHITE}}/경제설치 전환 <" + formatListString(selectedConverter.getDbTypes()) + ">");
                step = INTERNALSTEP.SELECT_DB;
            }
        }
    }

    private void start(String sender, String[] args) {
        if (args[0].equalsIgnoreCase("예")) {
            step = INTERNALSTEP.SELECT_CONVERT;
            Common.getInstance().getServerCaller().getPlayerCaller().sendMessage(sender, "{{DARK_GREEN}}현재 다음의 구조들을 지원합니다: {{WHITE}}" + getConverterListFormatted());
            Common.getInstance().getServerCaller().getPlayerCaller().sendMessage(sender, "{{WHITE}}/경제설치 전환 <" + getConverterListFormatted() + "> {{DARK_RED}}을 치세요");
        } else if (args[0].equalsIgnoreCase("아니요")) {
            Common.getInstance().getMainConfig().setValue("System.Setup", false);
            Common.getInstance().reloadPlugin();
            Common.getInstance().getServerCaller().getPlayerCaller().sendMessage(sender, "{{DARK_GREEN}}설치가 끝났습니다! Craftconomy를 즐기세요!");
        } else {
            Common.getInstance().getServerCaller().getPlayerCaller().sendMessage(sender, "{{DARK_RED}}올바른 값은 예 또는 아니요 입니다! {{WHITE}}/경제설치 전환 <예/아니요> {{DARK_RED}}를 치세요");
        }
    }

    private String getConverterListFormatted() {
        String result = "";
        Iterator<Entry<String, Converter>> iterator = IMPORTER_LIST.getConverterList().entrySet().iterator();
        while (iterator.hasNext()) {
            result += iterator.next().getKey();
            if (iterator.hasNext()) {
                result += ", ";
            }
        }
        return result;
    }

    private String formatListString(List<String> list) {
        String result = "";
        Iterator<String> iterator = list.iterator();
        while (iterator.hasNext()) {
            result += iterator.next();
            if (iterator.hasNext()) {
                result += ", ";
            }
        }
        return result;
    }
}
