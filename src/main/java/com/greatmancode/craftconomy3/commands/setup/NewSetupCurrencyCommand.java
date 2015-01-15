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
import com.greatmancode.craftconomy3.currency.Currency;
import com.greatmancode.tools.commands.interfaces.CommandExecutor;

import java.util.HashMap;
import java.util.Map;

public class NewSetupCurrencyCommand extends CommandExecutor {
    private enum INTERNALSTEP {
        NAME,
        NAMEPLURAL,
        MINOR,
        MINORPLURAL,
        SIGN;
    }

    private Map<String, String> map = new HashMap<String, String>();

    @Override
    public void execute(String sender, String[] args) {

        try {
            INTERNALSTEP step = INTERNALSTEP.valueOf(args[0].toUpperCase());

            if (step.equals(INTERNALSTEP.NAME)) {
                name(sender, args[1]);
            } else if (step.equals(INTERNALSTEP.NAMEPLURAL)) {
                namePlural(sender, args[1]);
            } else if (step.equals(INTERNALSTEP.MINOR)) {
                minor(sender, args[1]);
            } else if (step.equals(INTERNALSTEP.MINORPLURAL)) {
                minorPlural(sender, args[1]);
            } else if (step.equals(INTERNALSTEP.SIGN)) {
                sign(sender, args[1]);
            }
        } catch (IllegalArgumentException e) {
            Common.getInstance().getServerCaller().getPlayerCaller().sendMessage(sender, "{{DARK_RED}}올바르지 않은 하위-단계! 올바른거 하나를 작성하세요.");
        }
    }

    @Override
    public String help() {
        return "/경제설치 통화 - 첫번째 통화 구성";
    }

    @Override
    public int maxArgs() {
        return 2;
    }

    @Override
    public int minArgs() {
        return 2;
    }

    @Override
    public boolean playerOnly() {
        return false;
    }

    @Override
    public String getPermissionNode() {
        return "craftconomy.setup";
    }

    private void name(String sender, String name) {
        map.put("name", name);
        Common.getInstance().getServerCaller().getPlayerCaller().sendMessage(sender, "{{DARK_GREEN}}이제, {{WHITE}}복수형{{DARK_GREEN}}에 대한 통화 이름을 구성합시다 (Ex: {{WHITE}}Dollars{{DARK_GREEN}}). {{WHITE}}/경제설치 통화 nameplural <복수형> {{DARK_GREEN}}을 치세요");
        done(sender);
    }

    private void namePlural(String sender, String namePlural) {
        map.put("nameplural", namePlural);
        Common.getInstance().getServerCaller().getPlayerCaller().sendMessage(sender, "{{DARK_GREEN}}이제, {{WHITE}}소수점{{DARK_GREEN}}에 대한 통화 이름을 구성합시다 (Ex: {{WHITE}}Coin{{DARK_GREEN}}). {{WHITE}}/경제설치 통화 minor <소수점> {{DARK_GREEN}}을 치세요");
        done(sender);
    }

    private void minor(String sender, String minor) {
        map.put("minor", minor);
        Common.getInstance().getServerCaller().getPlayerCaller().sendMessage(sender, "{{DARK_GREEN}}이제, {{WHITE}}소수점 복수형{{DARK_GREEN}}에 대한 통화 이름을 구성합시다 (Ex: {{WHITE}}Coins{{DARK_GREEN}}). {{WHITE}}/경제설치 통화 minorplural <소수점 복수형> {{DARK_GREEN}}을 치세요");
        done(sender);
    }

    private void minorPlural(String sender, String minorPlural) {
        map.put("minorplural", minorPlural);
        Common.getInstance().getServerCaller().getPlayerCaller().sendMessage(sender, "{{DARK_GREEN}}마지막으로, 통화의 {{WHITE}}표식(기호){{DARK_GREEN}}을 넣습니다 (Ex: {{WHITE}}$ {{DARK_GREEN}}). {{WHITE}}/경제설치 통화 sign <표식> {{DARK_GREEN}}을 치세요");
        done(sender);
    }

    private void sign(String sender, String sign) {
        map.put("sign", sign);
        done(sender);
    }

    private void done(String sender) {
        if (map.size() == 5) {
            Currency currency = Common.getInstance().getCurrencyManager().addCurrency(map.get("name"), map.get("nameplural"), map.get("minor"), map.get("minorplural"), map.get("sign"), true);
            Common.getInstance().getCurrencyManager().setDefault(currency);
            Common.getInstance().getCurrencyManager().setDefaultBankCurrency(currency);
            Common.getInstance().getServerCaller().getPlayerCaller().sendMessage(sender, "{{DARK_GREEN}}우리는 이 단계를 마쳤습니다! 딱 2단계 남았습니다! {{WHITE}}/경제설치 기본 {{DARK_GREEN}}을 치세요");
            NewSetupWizard.setState(NewSetupWizard.BASIC_STEP);
        }
    }
}
