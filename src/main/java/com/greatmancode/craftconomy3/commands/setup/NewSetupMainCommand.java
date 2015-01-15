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

public class NewSetupMainCommand extends CommandExecutor {
    @Override
    public void execute(String sender, String[] args) {
        if (NewSetupWizard.getState().equals(NewSetupWizard.BASIC_STEP)) {
            start(sender);
        }
    }

    @Override
    public String help() {
        return "/경제설치 - 설치 마법사를 시작합니다.";
    }

    @Override
    public int maxArgs() {
        return 0;
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

    private void start(String sender) {
        Common.getInstance().getServerCaller().getPlayerCaller().sendMessage(sender, "{{WHITE}}Craftconomy 3 {{DARK_GREEN}} 설치 마법사에 오신 것을 환영합니다!");
        Common.getInstance().getServerCaller().getPlayerCaller().sendMessage(sender, "{{DARK_GREEN}}저는 당신이 {{WHITE}}Craftconomy {{DARK_GREEN}}를 당신이 원하는 대로 구성하도록 도와줍니다!");
        Common.getInstance().getServerCaller().getPlayerCaller().sendMessage(sender, "{{DARK_GREEN}}맨 먼저, 당신이 사용하길 원하는 데이터베이스 종류를 알기를 원합니다. 만약 {{WHITE}}flatfile {{DARK_GREEN}}데이터베이스를 사용하는 것을 원하신다면, 저는 {{WHITE}}H2 {{DARK_GREEN}}를 권장합니다.");
        Common.getInstance().getServerCaller().getPlayerCaller().sendMessage(sender, "{{WHITE}}/경제설치 데이터베이스 <mysql/h2> {{DARK_GREEN}}를 쳐주세요");
        NewSetupWizard.setState(NewSetupWizard.DATABASE_STEP);
    }
}
