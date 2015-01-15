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
import com.greatmancode.craftconomy3.DisplayFormat;
import com.greatmancode.craftconomy3.NewSetupWizard;
import com.greatmancode.tools.commands.interfaces.CommandExecutor;
import com.greatmancode.tools.utils.Tools;

public class NewSetupBasicCommand extends CommandExecutor {
    private enum INTERNALSTEP {
        DEFAULT_MONEY,
        BANK_PRICE,
        FORMAT,
        START;
    }

    private INTERNALSTEP step = INTERNALSTEP.START;

    @Override
    public void execute(String sender, String[] args) {
        if (NewSetupWizard.getState().equals(NewSetupWizard.BASIC_STEP)) {
            if (step.equals(INTERNALSTEP.START)) {
                start(sender);
            } else if (step.equals(INTERNALSTEP.DEFAULT_MONEY)) {
                defaultMoney(sender, args);
            } else if (step.equals(INTERNALSTEP.BANK_PRICE)) {
                bankMoney(sender, args);
            } else if (step.equals(INTERNALSTEP.FORMAT)) {
                format(sender, args);
            }
        }
    }

    @Override
    public String help() {
        return "/경제설치 기본 - 기본 명령어 설치 마법사.";
    }

    @Override
    public int maxArgs() {
        return 1;
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

    private void format(String sender, String[] args) {
        if (args.length == 1) {
            try {
                DisplayFormat format = DisplayFormat.valueOf(args[0].toUpperCase());
                Common.getInstance().getStorageHandler().getStorageEngine().setConfigEntry("longmode", format.toString());
                NewSetupWizard.setState(NewSetupWizard.CONVERT_STEP);
                Common.getInstance().loadDefaultSettings();
                Common.getInstance().startUp();
                Common.getInstance().getServerCaller().getPlayerCaller().sendMessage(sender, "{{DARK_GREEN}}딱 1단계 남았습니다! 다른 구조로부터 전환하기를 원하십니까? {{WHITE}} /경제설치 전환 예 {{DARK_GREEN}}또는 {{WHITE}}/경제설치 전환 아니요 를 치세요");
            } catch (IllegalArgumentException e) {
                Common.getInstance().getServerCaller().getPlayerCaller().sendMessage(sender, "{{DARK_RED}}이 보기 형식이 존재하지 않습니다! {{WHITE}}/경제설치 기본 <형식> 을 쳐주세요");
            }
        }
    }

    private void bankMoney(String sender, String[] args) {
        if (args.length == 1) {
            if (Tools.isValidDouble(args[0])) {
                Common.getInstance().getStorageHandler().getStorageEngine().setConfigEntry("bankprice", args[0]);
                step = INTERNALSTEP.FORMAT;
                Common.getInstance().getServerCaller().getPlayerCaller().sendMessage(sender, "{{DARK_GREEN}}이제, 당신이 원하는 금액이 보여질 형식을 선택하세요. Craftconomy는 {{WHITE}}4 {{DARK_GREEN}}가지의 보기 형식을 가지고 있습니다");
                Common.getInstance().getServerCaller().getPlayerCaller().sendMessage(sender, "{{WHITE}}Long{{DARK_GREEN}}: {{WHITE}}40 Dollars 1 Coin");
                Common.getInstance().getServerCaller().getPlayerCaller().sendMessage(sender, "{{WHITE}}Small{{DARK_GREEN}}: {{WHITE}} 40.1 Dollars");
                Common.getInstance().getServerCaller().getPlayerCaller().sendMessage(sender, "{{WHITE}}Sign{{DARK_GREEN}}: {{WHITE}} $40.1");
                Common.getInstance().getServerCaller().getPlayerCaller().sendMessage(sender, "{{WHITE}}MajorOnly{{DARK_GREEN}}: {{WHITE}}40 Dollars");
                Common.getInstance().getServerCaller().getPlayerCaller().sendMessage(sender, "{{WHITE}}/경제설치 기본 <형식> {{DARK_GREEN}}을 입력해주세요");
            } else {
                Common.getInstance().getServerCaller().getPlayerCaller().sendMessage(sender, "{{DARK_RED}}잘못된 수량! {{WHITE}}/경제설치 기본 <수량> {{DARK_RED}}을 치세요");
            }
        } else {
           Common.getInstance().getServerCaller().getPlayerCaller().sendMessage(sender, "{{DARK_RED}}돈의 수량을 입력해야 합니다! {{WHITE}}/경제설치 기본 <수량> {{DARK_RED}}을 치세요");
        }
    }

    private void defaultMoney(String sender, String[] args) {
        if (args.length == 1) {
            if (Tools.isValidDouble(args[0])) {
                Common.getInstance().getStorageHandler().getStorageEngine().setConfigEntry("holdings", args[0]);
                step = INTERNALSTEP.BANK_PRICE;
                Common.getInstance().getServerCaller().getPlayerCaller().sendMessage(sender, "{{DARK_GREEN}}플레이어가 {{WHITE}}은행 계좌{{DARK_GREEN}}에 얼마나 지불하기를 원하십니까? {{WHITE}}/경제설치 기본 <수량> {{DARK_RED}}을 치세요");
            } else {
                Common.getInstance().getServerCaller().getPlayerCaller().sendMessage(sender, "{{DARK_RED}}잘못된 수량! {{WHITE}}/경제설치 기본 <수량> {{DARK_RED}}을 치세요");
            }
        } else {
            Common.getInstance().getServerCaller().getPlayerCaller().sendMessage(sender, "{{DARK_RED}}돈의 수량을 입력해야 합니다! {{WHITE}}/경제설치 기본 <수량> {{DARK_RED}}을 치세요");
        }
    }

    private void start(String sender) {
        Common.getInstance().getServerCaller().getPlayerCaller().sendMessage(sender, "{{DARK_GREEN}}기본 설치. 이 단계에서는, Craftconomy의 기본 설정을 구성할 수 있습니다.");
        Common.getInstance().getServerCaller().getPlayerCaller().sendMessage(sender, "{{DARK_GREEN}}플레이어가 초기 자금을 얼마나 가지길 원합니까? {{WHITE}}/경제설치 기본 <수량> {{DARK_GREEN}}을 치세요");
        step = INTERNALSTEP.DEFAULT_MONEY;
    }
}
