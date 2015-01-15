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
package com.greatmancode.craftconomy3;

import com.greatmancode.craftconomy3.account.Account;
import com.greatmancode.craftconomy3.account.AccountManager;
import com.greatmancode.craftconomy3.commands.bank.*;
import com.greatmancode.craftconomy3.commands.config.*;
import com.greatmancode.craftconomy3.commands.currency.*;
import com.greatmancode.craftconomy3.commands.group.GroupAddWorldCommand;
import com.greatmancode.craftconomy3.commands.group.GroupCreateCommand;
import com.greatmancode.craftconomy3.commands.group.GroupDelWorldCommand;
import com.greatmancode.craftconomy3.commands.money.*;
import com.greatmancode.craftconomy3.commands.setup.*;
import com.greatmancode.craftconomy3.currency.Currency;
import com.greatmancode.craftconomy3.currency.CurrencyManager;
import com.greatmancode.craftconomy3.events.EventManager;
import com.greatmancode.craftconomy3.groups.WorldGroupsManager;
import com.greatmancode.craftconomy3.storage.StorageHandler;
import com.greatmancode.craftconomy3.utils.OldFormatConverter;
import com.greatmancode.tools.caller.bukkit.BukkitServerCaller;
import com.greatmancode.tools.caller.unittest.UnitTestServerCaller;
import com.greatmancode.tools.commands.CommandHandler;
import com.greatmancode.tools.commands.SubCommand;
import com.greatmancode.tools.configuration.Config;
import com.greatmancode.tools.configuration.ConfigurationManager;
import com.greatmancode.tools.interfaces.caller.ServerCaller;
import com.greatmancode.tools.language.LanguageManager;
import com.greatmancode.tools.utils.Metrics;
import com.greatmancode.tools.utils.Tools;
import com.greatmancode.tools.utils.Updater;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The core of Craftconomy. Every requests pass through this class
 *
 * @author greatman
 */
public class Common implements com.greatmancode.tools.interfaces.Common {
    private Logger log = null;
    private static Common instance = null;
    // Managers
    private AccountManager accountManager = null;
    private ConfigurationManager config = null;
    private CurrencyManager currencyManager = null;
    private StorageHandler storageHandler = null;
    private EventManager eventManager = null;
    private LanguageManager languageManager = null;
    private WorldGroupsManager worldGroupManager = null;
    private CommandHandler commandManager = null;
    private ServerCaller serverCaller = null;
    private boolean databaseInitialized = false;
    private boolean currencyInitialized = false;
    private static boolean initialized = false;
    private Metrics metrics = null;
    private Config mainConfig = null;
    private Updater updater;
    //Default values
    private DisplayFormat displayFormat = null;
    private double holdings = 0.0;
    private double bankPrice = 0.0;

    /**
     * Initialize the Common core.
     */
    public void onEnable(ServerCaller serverCaller, final Logger log) {
        this.serverCaller = serverCaller;
        instance = this;
        this.log = log;
        if (!initialized) {
            sendConsoleMessage(Level.INFO, "시작합니다!");
            sendConsoleMessage(Level.INFO, "구성을 불러오는 중");
            config = new ConfigurationManager(serverCaller);
            mainConfig = config.loadFile(serverCaller.getDataFolder(), "config.yml");
            if (!mainConfig.has("System.Setup")) {
                initializeConfig();
            }
            if (!getMainConfig().has("System.Database.Prefix")) {
                getMainConfig().setValue("System.Database.Prefix", "cc3_");
            }

            languageManager = new LanguageManager(serverCaller, serverCaller.getDataFolder(), "lang.yml");
            loadLanguage();
            serverCaller.setCommandPrefix(languageManager.getString("command_prefix"));
            if (!(getServerCaller() instanceof UnitTestServerCaller)) {
                try {
                    metrics = new Metrics("Craftconomy", this.getServerCaller().getPluginVersion(), serverCaller);
                } catch (IOException e) {
                    this.getLogger().log(Level.SEVERE, String.format(getLanguageManager().getString("metric_start_error"), e.getMessage()));
                }
            }
            if (getMainConfig().getBoolean("System.CheckNewVersion") && (serverCaller instanceof BukkitServerCaller)) {
                updater = new Updater(serverCaller, 35564, Updater.UpdateType.NO_DOWNLOAD, false);
                if (updater.getResult() == Updater.UpdateResult.UPDATE_AVAILABLE) {
                    sendConsoleMessage(Level.WARNING, getLanguageManager().parse("running_old_version", updater.getLatestName()));
                }
            }
            sendConsoleMessage(Level.INFO, "리스너를 불러오는 중.");
            serverCaller.getLoader().getEventManager().registerEvents(this, new EventManager());
            sendConsoleMessage(Level.INFO, "명령어를 불러오는 중");
            Common.getInstance().getServerCaller().registerPermission("craftconomy.*");
            commandManager = new CommandHandler(serverCaller);
            registerCommands();
            if (getMainConfig().getBoolean("System.Setup")) {

                //We got quick setup. Let's do it!!!!
                if (getMainConfig().getBoolean("System.QuickSetup.Enable")) {
                    quickSetup();
                    reloadPlugin();
                } else {
                    sendConsoleMessage(Level.WARNING, getLanguageManager().getString("loaded_setup_mode"));
                }
            } else {
                commandManager.setLevel(1);
                initialiseDatabase();
                updateDatabase();
                initializeCurrency();
                sendConsoleMessage(Level.INFO, getLanguageManager().getString("loading_default_settings"));
                loadDefaultSettings();
                sendConsoleMessage(Level.INFO, getLanguageManager().getString("default_settings_loaded"));
                startUp();
                sendConsoleMessage(Level.INFO, getLanguageManager().getString("ready"));
            }


            getServerCaller().registerPermission("craftconomy.money.log.others");
            initialized = true;
        }
    }

    /**
     * Disable the plugin.
     */
    @Override
    public void onDisable() {
        if (getStorageHandler() != null) {
            getLogger().info(getLanguageManager().getString("closing_db_link"));
            getStorageHandler().disable();
        }
        // Managers
        accountManager = null;
        config = null;
        currencyManager = null;
        storageHandler = null;
        eventManager = null;
        languageManager = null;
        worldGroupManager = null;
        commandManager = null;
        databaseInitialized = false;
        currencyInitialized = false;
        initialized = false;
        metrics = null;
        mainConfig = null;
        updater = null;
        //Default values
        displayFormat = null;
        holdings = 0.0;
        bankPrice = 0.0;
    }

    /**
     * Reload the plugin.
     */
    public void reloadPlugin() {
        sendConsoleMessage(Level.INFO, "시작합니다!");
        sendConsoleMessage(Level.INFO, "구성을 불러오는 중");
        config = new ConfigurationManager(serverCaller);
        mainConfig = config.loadFile(serverCaller.getDataFolder(), "config.yml");
        if (!mainConfig.has("System.Setup")) {
            initializeConfig();
        }
        if (!getMainConfig().has("System.Database.Prefix")) {
            getMainConfig().setValue("System.Database.Prefix", "cc3_");
        }

        languageManager = new LanguageManager(serverCaller, serverCaller.getDataFolder(), "lang.yml");
        loadLanguage();
        serverCaller.setCommandPrefix(languageManager.getString("command_prefix"));
        commandManager = new CommandHandler(serverCaller);
        registerCommands();
        commandManager.setLevel(1);
        initialiseDatabase();
        updateDatabase();
        initializeCurrency();
        sendConsoleMessage(Level.INFO, getLanguageManager().getString("loading_default_settings"));
        loadDefaultSettings();
        sendConsoleMessage(Level.INFO, getLanguageManager().getString("default_settings_loaded"));
        startUp();
        sendConsoleMessage(Level.INFO, getLanguageManager().getString("ready"));

    }

    /**
     * Retrieve the main configuration file
     *
     * @return the main configuration file
     */
    public Config getMainConfig() {
        return mainConfig;
    }

    /**
     * Retrieve the logger associated with this plugin.
     *
     * @return The logger instance.
     */
    public Logger getLogger() {
        return log;
    }

    /**
     * Sends a message to the console through the Logge.r
     *
     * @param level The log level to show.
     * @param msg   The message to send.
     */
    public void sendConsoleMessage(Level level, String msg) {
        if (!(getServerCaller() instanceof UnitTestServerCaller)) {
            getLogger().log(level, msg);
        }
    }

    /**
     * Retrieve the instance of Common. Need to go through that to access any managers.
     *
     * @return The Common instance.
     */
    public static Common getInstance() {
        return instance;
    }

    /**
     * Retrieve the Account Manager.
     *
     * @return The Account Manager instance or null if the manager is not initialized.
     */
    public AccountManager getAccountManager() {
        return accountManager;
    }

    /**
     * Retrieve the Configuration Manager.
     *
     * @return The Configuration Manager instance or null if the manager is not initialized.
     */
    public ConfigurationManager getConfigurationManager() {
        return config;
    }

    /**
     * Retrieve the Storage Handler.
     *
     * @return The Storage Handler instance or null if the handler is not initialized.
     */
    public StorageHandler getStorageHandler() {
        return storageHandler;
    }

    /**
     * Retrieve the Currency Manager.
     *
     * @return The Currency Manager instance or null if the manager is not initialized.
     */
    public CurrencyManager getCurrencyManager() {
        return currencyManager;
    }

    /**
     * Retrieve the Command Manager.
     *
     * @return The Command Manager instance or null if the manager is not initialized.
     */
    public CommandHandler getCommandManager() {
        return commandManager;
    }

    /**
     * Retrieve the Server Caller.
     *
     * @return The Server Caller instance or null if the caller is not initialized.
     */
    public ServerCaller getServerCaller() {
        return serverCaller;
    }

    /**
     * Format a balance to a readable string.
     *
     * @param worldName The world Name associated with this balance
     * @param currency  The currency instance associated with this balance.
     * @param balance   The balance.
     * @param format    the display format to use
     * @return A pretty String showing the balance. Returns a empty string if currency is invalid.
     */
    public String format(String worldName, Currency currency, double balance, DisplayFormat format) {
        StringBuilder string = new StringBuilder();

        if (worldName != null && !worldName.equals(WorldGroupsManager.DEFAULT_GROUP_NAME)) {
            // We put the world name if the conf is true
            string.append(worldName).append(": ");
        }
        if (currency != null) {
            // We removes some cents if it's something like 20.20381 it would set it
            // to 20.20

            String[] theAmount = BigDecimal.valueOf(balance).toPlainString().split("\\.");
            DecimalFormatSymbols unusualSymbols = new DecimalFormatSymbols();
            unusualSymbols.setGroupingSeparator(',');
            DecimalFormat decimalFormat = new DecimalFormat("###,###", unusualSymbols);
            String name = currency.getName();
            if (balance > 1.0 || balance < 1.0) {
                name = currency.getPlural();
            }
            String coin;
            if (theAmount.length == 2) {
                if (theAmount[1].length() >= 2) {
                    coin = theAmount[1].substring(0, 2);
                } else {
                    coin = theAmount[1] + "0";
                }
            } else {
                coin = "0";
            }
            String amount;
            try {
                amount = decimalFormat.format(Double.parseDouble(theAmount[0]));
            } catch (NumberFormatException e) {
                amount = theAmount[0];
            }

            // Do we seperate money and dollar or not?
            if (format == DisplayFormat.LONG) {
                String subName = currency.getMinor();
                if (Long.parseLong(coin) > 1) {
                    subName = currency.getMinorPlural();
                }
                string.append(amount).append(" ").append(name).append(" ").append(coin).append(" ").append(subName);
            } else if (format == DisplayFormat.SMALL) {
                string.append(amount).append(".").append(coin).append(" ").append(name);
            } else if (format == DisplayFormat.SIGN) {
                string.append(currency.getSign()).append(amount).append(".").append(coin);
            } else if (format == DisplayFormat.MAJORONLY) {
                string.append(amount).append(" ").append(name);
            }
        }
        return string.toString();
    }

    /**
     * Format a balance to a readable string with the default formatting.
     *
     * @param worldName The world Name associated with this balance
     * @param currency  The currency instance associated with this balance.
     * @param balance   The balance.
     * @return A pretty String showing the balance. Returns a empty string if currency is invalid.
     */
    public String format(String worldName, Currency currency, double balance) {
        return format(worldName, currency, balance, displayFormat);
    }

    /**
     * Initialize the database Manager
     */
    public void initialiseDatabase() {
        if (!databaseInitialized) {
            sendConsoleMessage(Level.INFO, getLanguageManager().getString("loading_database_manager"));
            storageHandler = new StorageHandler();

            //TODO: Re-support that
            /*if (getMainConfig().getBoolean("System.Database.ConvertFromSQLite")) {
                convertDatabase(dbManager);
            }*/
            databaseInitialized = true;
            sendConsoleMessage(Level.INFO, getLanguageManager().getString("database_manager_loaded"));
        }
    }

    /**
     * Convert from SQLite to MySQL
     *
     * @param dbManagernew The MySQL instance
     */
    /*private void convertDatabase(DatabaseManager dbManagernew) throws InvalidDatabaseConstructor {
        sendConsoleMessage(Level.INFO, getLanguageManager().getString("starting_database_convert"));
        new SQLiteToMySQLConverter().run();
        sendConsoleMessage(Level.INFO, getLanguageManager().getString("convert_done"));
        getMainConfig().setValue("System.Database.ConvertFromSQLite", false);
    }*/

    /**
     * Initialize the {@link CurrencyManager}
     */
    public void initializeCurrency() {
        if (!currencyInitialized) {
            sendConsoleMessage(Level.INFO, getLanguageManager().getString("loading_currency_manager"));
            currencyManager = new CurrencyManager();
            currencyInitialized = true;
            sendConsoleMessage(Level.INFO, getLanguageManager().getString("currency_manager_loaded"));
        }
    }

    /**
     * Initialize the {@link WorldGroupsManager}
     */
    public void initializeWorldGroup() {
        if (worldGroupManager == null) {
            worldGroupManager = new WorldGroupsManager();
            sendConsoleMessage(Level.INFO, getLanguageManager().getString("world_group_manager_loaded"));
        }
    }

    /**
     * Initialize the {@link AccountManager}, Metrics and {@link EventManager}
     */
    public void startUp() {
        sendConsoleMessage(Level.INFO, getLanguageManager().getString("loading_account_manager"));
        accountManager = new AccountManager();
        //addMetricsGraph("Multiworld", getConfigurationManager().isMultiWorld());
        startMetrics();
        sendConsoleMessage(Level.INFO, getLanguageManager().getString("account_manager_loaded"));
        eventManager = new EventManager();
        initializeWorldGroup();
    }

    /**
     * Add a graph to Metrics
     *
     * @param title The title of the Graph
     * @param value The value of the entry
     */
    public void addMetricsGraph(String title, String value) {
        if (metrics != null) {
            Metrics.Graph graph = metrics.createGraph(title);
            graph.addPlotter(new Metrics.Plotter(value) {
                @Override
                public int getValue() {
                    return 1;
                }
            });
        }
    }

    /**
     * Add a graph to Metrics
     *
     * @param title The title of the Graph
     * @param value The value of the entry
     */
    public void addMetricsGraph(String title, boolean value) {
        addMetricsGraph(title, value ? "Yes" : "No");
    }

    /**
     * Start Metrics.
     */
    public void startMetrics() {
        if (metrics != null) {
            getLogger().info("Starting Metrics.");
            metrics.start();
        }
    }

    /**
     * Write a transaction to the Log.
     *
     * @param info        The type of transaction to log.
     * @param cause       The cause of the transaction.
     * @param causeReason The reason of the cause
     * @param account     The account being impacted by the change
     * @param amount      The amount of money in this transaction.
     * @param currency    The currency associated with this transaction
     * @param worldName   The world name associated with this transaction
     */
    public void writeLog(LogInfo info, Cause cause, String causeReason, Account account, double amount, Currency currency, String worldName) {
        if (getMainConfig().getBoolean("System.Logging.Enabled")) {
            getStorageHandler().getStorageEngine().saveLog(info, cause, causeReason, account, amount, currency, worldName);
        }
    }

    /**
     * Get the version Checker.
     *
     * @return The version checker. May return null if the system is disabled in the config.yml
     */
    public Updater getVersionChecker() {
        return updater;
    }

    /**
     * Retrieve the Event manager.
     *
     * @return The Event manager.
     */
    public EventManager getEventManager() {
        return eventManager;
    }

    /**
     * Retrieve the {@link com.greatmancode.tools.language.LanguageManager}
     *
     * @return The {@link com.greatmancode.tools.language.LanguageManager}
     */
    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    /**
     * Retrieve the {@link WorldGroupsManager}
     *
     * @return The {@link WorldGroupsManager}
     */
    public WorldGroupsManager getWorldGroupManager() {
        return worldGroupManager;
    }

    /**
     * Check if the system has been initialized.
     *
     * @return True if the system has been initialized else false.
     */
    public static boolean isInitialized() {
        return initialized;
    }

    /**
     * Reload the default settings.
     */
    public void loadDefaultSettings() {
        String value = getStorageHandler().getStorageEngine().getConfigEntry("longmode");
        if (value != null) {
            displayFormat = DisplayFormat.valueOf(value.toUpperCase());
        } else {
            getStorageHandler().getStorageEngine().setConfigEntry("longmode", "long");
            displayFormat = DisplayFormat.LONG;
        }
        addMetricsGraph("Display Format", displayFormat.toString());
        value = getStorageHandler().getStorageEngine().getConfigEntry("holdings");
        if (value != null && Tools.isValidDouble(value)) {
            holdings = Double.parseDouble(value);
        } else {
            getStorageHandler().getStorageEngine().setConfigEntry("holdings", 100.0 + "");
            sendConsoleMessage(Level.SEVERE, "No default value was set for account creation or was invalid! Defaulting to 100.");
            holdings = 100.0;
        }
        value = getStorageHandler().getStorageEngine().getConfigEntry("bankprice");
        if (value != null && Tools.isValidDouble(value)) {
            bankPrice = Double.parseDouble(value);
        } else {
            getStorageHandler().getStorageEngine().setConfigEntry("bankprice", 100.0 + "");
            sendConsoleMessage(Level.SEVERE, "No default value was set for bank creation or was invalid! Defaulting to 100.");
            bankPrice = 100.0;
        }
    }

    /**
     * Retrieve the display format for any formatting through {@link #format(String, com.greatmancode.craftconomy3.currency.Currency, double, DisplayFormat)}
     *
     * @return the display format used.
     */
    public DisplayFormat getDisplayFormat() {
        return displayFormat;
    }

    /**
     * Set the display format for any formatting through {@link #format(String, com.greatmancode.craftconomy3.currency.Currency, double, DisplayFormat)}
     *
     * @param format The format display to be set to
     */
    public void setDisplayFormat(DisplayFormat format) {
        getStorageHandler().getStorageEngine().setConfigEntry("longmode", format.toString());
        displayFormat = format;
    }

    /**
     * Get the default amount of money a account will have
     *
     * @return the default amount of money
     */
    public double getDefaultHoldings() {
        return holdings;
    }

    /**
     * Set the default amount of money a account will have
     *
     * @param value the default amount of money
     */
    public void setDefaultHoldings(double value) {
        getStorageHandler().getStorageEngine().setConfigEntry("holdings", String.valueOf(value));
        holdings = value;
    }

    /**
     * Retrieve the price of a bank account creation
     *
     * @return The price of a bank account creation
     */
    public double getBankPrice() {
        return bankPrice;
    }

    /**
     * Set the bank account creation price
     *
     * @param value the bank account creation price
     */
    public void setBankPrice(double value) {
        getStorageHandler().getStorageEngine().setConfigEntry("bankprice", String.valueOf(value));
        bankPrice = value;
    }

    /**
     * Perform a quick setup
     */
    private void quickSetup() {
        initialiseDatabase();
        Common.getInstance().initializeCurrency();
        Currency currency = Common.getInstance().getCurrencyManager().addCurrency(getMainConfig().getString("System.QuickSetup.Currency.Name"), getMainConfig().getString("System.QuickSetup.Currency.NamePlural"), getMainConfig().getString("System.QuickSetup.Currency.Minor"), getMainConfig().getString("System.QuickSetup.Currency.MinorPlural"), getMainConfig().getString("System.QuickSetup.Currency.Sign"), true);
        Common.getInstance().getCurrencyManager().setDefault(currency);
        Common.getInstance().getCurrencyManager().setDefaultBankCurrency(currency);
        getStorageHandler().getStorageEngine().setConfigEntry("longmode", DisplayFormat.valueOf(getMainConfig().getString("System.QuickSetup.DisplayMode").toUpperCase()).toString());
        getStorageHandler().getStorageEngine().setConfigEntry("holdings", getMainConfig().getString("System.QuickSetup.StartBalance"));
        getStorageHandler().getStorageEngine().setConfigEntry("bankprice", getMainConfig().getString("System.QuickSetup.PriceBank"));
        initializeCurrency();
        loadDefaultSettings();
        Common.getInstance().startUp();
        Common.getInstance().getMainConfig().setValue("System.Setup", false);
        commandManager.setLevel(1);
        sendConsoleMessage(Level.INFO, "빠른-구성 완료!");
    }

    /**
     * Register all the commands
     */
    private void registerCommands() {
        commandManager.setWrongLevelMsg(languageManager.getString("command_disabled_setup_mode"));
        SubCommand money = new SubCommand("돈", commandManager, null, 1);
        money.addCommand("", new MainCommand());
        money.addCommand("모두", new AllCommand());
        money.addCommand("지불", new PayCommand());
        money.addCommand("주기", new GiveCommand());
        money.addCommand("뺏기", new TakeCommand());
        money.addCommand("설정", new SetCommand());
        money.addCommand("삭제", new DeleteCommand());
        money.addCommand("생성", new CreateCommand());
        money.addCommand("잔액", new BalanceCommand());
        money.addCommand("순위", new TopCommand());
        money.addCommand("교환", new ExchangeCommand());
        money.addCommand("무한", new InfiniteCommand());
        money.addCommand("기록", new LogCommand());
        commandManager.registerMainCommand("돈", money);

        SubCommand bank = new SubCommand("은행", commandManager, null, 1);
        bank.addCommand("생성", new BankCreateCommand());
        bank.addCommand("잔액", new BankBalanceCommand());
        bank.addCommand("입금", new BankDepositCommand());
        bank.addCommand("출금", new BankWithdrawCommand());
        bank.addCommand("설정", new BankSetCommand());
        bank.addCommand("주기", new BankGiveCommand());
        bank.addCommand("뺏기", new BankTakeCommand());
        bank.addCommand("권한", new BankPermCommand());
        bank.addCommand("목록", new BankListCommand());
        bank.addCommand("삭제", new BankDeleteCommand());
        bank.addCommand("acl무시", new BankIgnoreACLCommand());
        commandManager.registerMainCommand("은행", bank);

        SubCommand ccsetup = new SubCommand("경제설치", commandManager, null, 0);
        ccsetup.addCommand("", new NewSetupMainCommand());
        ccsetup.addCommand("데이터베이스", new NewSetupDatabaseCommand());
        ccsetup.addCommand("통화", new NewSetupCurrencyCommand());
        ccsetup.addCommand("기본", new NewSetupBasicCommand());
        ccsetup.addCommand("전환", new NewSetupConvertCommand());
        commandManager.registerMainCommand("경제설치", ccsetup);

        SubCommand currency = new SubCommand("통화", commandManager, null, 1);
        currency.addCommand("추가", new CurrencyAddCommand());
        currency.addCommand("삭제", new CurrencyDeleteCommand());
        currency.addCommand("편집", new CurrencyEditCommand());
        currency.addCommand("정보", new CurrencyInfoCommand());
        currency.addCommand("기본", new CurrencyDefaultCommand());
        currency.addCommand("교환", new CurrencyExchangeCommand());
        currency.addCommand("금리", new CurrencyRatesCommand());
        currency.addCommand("목록", new CurrencyListCommand());
        commandManager.registerMainCommand("통화", currency);

        SubCommand configCommand = new SubCommand("경제", commandManager, null, 1);
        configCommand.addCommand("재산", new ConfigHoldingsCommand());
        configCommand.addCommand("은행가격", new ConfigBankPriceCommand());
        configCommand.addCommand("형식", new ConfigFormatCommand());
        configCommand.addCommand("기록청소", new ConfigClearLogCommand());
        configCommand.addCommand("갱신", new ConfigReloadCommand());
        commandManager.registerMainCommand("경제", configCommand);

        SubCommand ccgroup = new SubCommand("경제연합", commandManager, null, 1);
        ccgroup.addCommand("생성", new GroupCreateCommand());
        ccgroup.addCommand("세계추가", new GroupAddWorldCommand());
        ccgroup.addCommand("세계삭제", new GroupDelWorldCommand());
        commandManager.registerMainCommand("경제연합", ccgroup);

        SubCommand payCommand = new SubCommand("지불", commandManager, null, 1);
        payCommand.addCommand("", new PayCommand());
        commandManager.registerMainCommand("지불", payCommand);
    }

    /**
     * Initialize the configuration file
     */
    private void loadLanguage() {
        languageManager.addLanguageEntry("metric_start_error", "Metrics를 불러올 수 없습니다! 오류: %s");
        languageManager.addLanguageEntry("checking_new_version", "새 버전이 있는지 확인합니다.");
        languageManager.addLanguageEntry("running_old_version", "구버전의 Craftconomy를 사용중입니다! 새 버전은: %s");
        languageManager.addLanguageEntry("database_connect_error", "데이터베이스에 연결 시도 중 오류가 발생했습니다. 수집된 메세지: %s");
        languageManager.addLanguageEntry("loading_default_settings", "기본 설정 불러오는 중.");
        languageManager.addLanguageEntry("default_settings_loaded", "기본 설정 불러와짐!");
        languageManager.addLanguageEntry("loaded_setup_mode", "Craftconomy를 설치 모드로 불러오는 중. /경제설치 를 입력하여 설치를 시작하세요.");
        languageManager.addLanguageEntry("ready", "준비!");
        languageManager.addLanguageEntry("closing_db_link", "데이터베이스 연결 종료 중.");
        languageManager.addLanguageEntry("unable_close_db_link", "연결을 닫을 데이터베이스가 존재하지 않습니다! 이유: %s");
        languageManager.addLanguageEntry("loading_database_manager", "데이터베이스 관리자 불러오는 중");
        languageManager.addLanguageEntry("database_manager_loaded", "데이터베이스 관리자 불러와짐!");
        languageManager.addLanguageEntry("loading_curency_manager", "통화 관리자 불러오는 중");
        languageManager.addLanguageEntry("currency_manager_loaded", "통화 관리자 불러와짐!");
        languageManager.addLanguageEntry("loading_account_manager", "계좌 관리자 불러오는 중");
        languageManager.addLanguageEntry("account_manager_loaded", "계좌 관리자 불러와짐!");
        languageManager.addLanguageEntry("loading_payday_manager", "지급일 관리자 불러오는 중.");
        languageManager.addLanguageEntry("payday_manager_loaded", "지급일 관리자 불러와짐!");
        languageManager.addLanguageEntry("error_write_log", "거래 기록을 작성하는 중 오류가 발생했습니다! 오류: %s");
        languageManager.addLanguageEntry("invalid_library", "라이브러리 URL이 올바르지 않습니다: %s. 전체 오류는: %s");
        languageManager.addLanguageEntry("command_disabled_setup_mode", "{{DARK_RED}}이 명령어는 Craftconomy가 설치 모드 안에 있는 동안 비활성화됩니다! /경제설치 로 플러그인을 구성하세요.");
        languageManager.addLanguageEntry("user_only_command", "{{DARK_RED}}이 명령어는 사용자만 사용할 수 있습니다!");
        languageManager.addLanguageEntry("no_permission", "{{DARK_RED}}당신은 권한을 가지고 있지 않습니다!");
        languageManager.addLanguageEntry("command_usage", "사용법: %s");
        languageManager.addLanguageEntry("subcommand_not_exist", "{{DARK_RED}}이 보조 명령어는 존재하지 않습니다!");
        languageManager.addLanguageEntry("bank_statement", "{{DARK_GREEN}}은행 상태:");
        languageManager.addLanguageEntry("cant_check_bank_statement", "{{DARK_RED}}이 은행 계좌의 상태를 확인할 수 없습니다");
        languageManager.addLanguageEntry("account_not_exist", "{{DARK_RED}}이 계좌은 존재하지 않습니다!");
        languageManager.addLanguageEntry("bank_account_created", "{{DARK_GREEN}}계좌가 생성되었습니다!");
        languageManager.addLanguageEntry("bank_account_not_enough_money_create", "{{DARK_RED}}당신은 은행 계좌를 만들 충분한 돈을 가지고 있지 않습니다! {{WHITE}}%s(이)가 필요합니다");
        languageManager.addLanguageEntry("account_already_exists", "{{DARK_RED}}이 계좌는 이미 존재합니다!");
        languageManager.addLanguageEntry("currency_not_exist", "{{DARK_RED}}그 통화는 이미 존재합니다!");
        languageManager.addLanguageEntry("not_enough_money", "{{DARK_RED}}돈이 충분하지 않습니다!");
        languageManager.addLanguageEntry("invalid_amount", "{{DARK_RED}}수량이 올바르지 않습니다!");
        languageManager.addLanguageEntry("bank_cant_deposit", "{{DARK_RED}}이 계좌에 입금할 수 없습니다!");
        languageManager.addLanguageEntry("deposited", "{{WHITE}}%s {{DARK_GREEN}}을/를 {{WHITE}}%s{{DARK_GREEN}} 은행 계좌에 입금했습니다.");
        languageManager.addLanguageEntry("bank_help_title", "{{DARK_GREEN}} ======== 은행 명령어 ========");
        languageManager.addLanguageEntry("bank_create_cmd_help", "/은행 생성 <계좌 이름> - 은행 계좌를 만들기");
        languageManager.addLanguageEntry("bank_balance_cmd_help", "/은행 잔액 <계좌 이름> - 계좌의 잔액을 확인하기.");
        languageManager.addLanguageEntry("bank_deposit_cmd_help", "/은행 입금 <계좌 이름> <수량> [통화] - 은행 계좌에 돈을 입금하기.");
        languageManager.addLanguageEntry("bank_give_cmd_help", "/은행 주기 <계좌 이름> <수량> [통화] [세계] - 은행 계좌에 돈을 주기.");
        languageManager.addLanguageEntry("bank_help_cmd_help", "/은행 - 은행 도움말을 봅니다");
        languageManager.addLanguageEntry("bank_perm_cmd_help", "/은행 권한 <계좌 이름> <입금/츌금/acl/보기> <플레이어 이름> <true/false> - 플레이어의 권한을 수정하기");
        languageManager.addLanguageEntry("bank_set_cmd_help", "/은행 설정 <계좌 이름> <수량> [통화] [세계]- 계좌의 잔고를 설정하기.");
        languageManager.addLanguageEntry("bank_take_cmd_help", "/은행 뺏기 <계좌 이름> <수량> [통화] [세계]- 계좌에서 돈을 뺏기.");
        languageManager.addLanguageEntry("bank_withdraw_cmd_help", "/은행 출금 <계좌 이름> <수량> [통화] - 계좌에서 돈을 출금하기.");
        languageManager.addLanguageEntry("world_not_exist", "{{DARK_RED}}이 세계는 존재하지 않습니다!");
        languageManager.addLanguageEntry("bank_give_success", "{{WHITE}}%s {{DARK_GREEN}}을/를 {{WHITE}}%s{{DARK_GREEN}} 은행 계좌로부터 입금했습니다.");
        languageManager.addLanguageEntry("invalid_flag", "{{DARK_RED}}올바르지 않은 플래그!");
        languageManager.addLanguageEntry("bank_flag_set", "{{WHITE}}%s {{DARK_GREEN}}플래그를 {{WHITE}}%s {{DARK_GREEN}}님이 {{WHITE}}%s{{DARK_GREEN}}로 설정했습니다");
        languageManager.addLanguageEntry("cant_modify_acl", "{{DARK_RED}}이 계좌의 ACL를 수정할 수 없습니다!");
        languageManager.addLanguageEntry("bank_set_success", "{{WHITE}}%s {{DARK_GREEN}}을/를 {{WHITE}}%s {{DARK_GREEN}}은행 계좌로 설정했습니다.");
        languageManager.addLanguageEntry("bank_not_enough_money", "{{DARK_RED}}그 은행 계좌는 충분한 돈을 가지고 있지 않습니다!");
        languageManager.addLanguageEntry("bank_take_success", "{{WHITE}}%s {{DARK_GREEN}}을/를 {{WHITE}}%s {{DARK_GREEN}}은행 계좌로부터 빼갔습니다.");
        languageManager.addLanguageEntry("cant_withdraw_bank", "{{DARK_RED}}이 계좌에서 출금할 수 없습니다!");
        languageManager.addLanguageEntry("bank_price_modified", "{{DARK_GREEN}}은행 가격 수정됨!");
        languageManager.addLanguageEntry("config_bankprice_cmd_help", "/경제 은행가격 <수량> - 은행 계좌 생성 비용을 변경하기.");
        languageManager.addLanguageEntry("config_format_cmd_help", "/경제 형식 <long/small/sign/majoronly> - 보기 형식을 설정하기.");
        languageManager.addLanguageEntry("config_cmd_help", "/경제 - 구성 명령어 도움말 보기");
        languageManager.addLanguageEntry("config_holdings_cmd_help", "/경제 재산 <수량> - 사용자 계정의 돈의 기본 수량을 설정합니다.");
        languageManager.addLanguageEntry("config_help_title", "{{DARK_GREEN}} ======== 경제 명령어 ========");
        languageManager.addLanguageEntry("format_modified", "{{DARK_GREEN}}long 금액 형식 변경됨!");
        languageManager.addLanguageEntry("invalid_mode", "{{DARK_RED}}올바르지 않은 모드!");
        languageManager.addLanguageEntry("default_holding_modified", "{{DARK_GREEN}}기본 재산 수정됨!");
        languageManager.addLanguageEntry("currency_added", "{{DARK_GREEN}}통화 추가됨!");
        languageManager.addLanguageEntry("currency_already_exists", "{{DARK_RED}}이 통화는 이미 존재합니다!");
        languageManager.addLanguageEntry("currency_add_cmd_help", "/통화 추가 <이름> <이름 복수형> <소수점> <소수점 복수형> <표식> - 통화 추가하기.");
        languageManager.addLanguageEntry("currency_default_cmd_help", "/통화 기본 <이름> - 기본 통화 설정하기.");
        languageManager.addLanguageEntry("currency_delete_cmd_help", "/통화 삭제 <이름> - 통화 삭제하기 {{DARK_RED}}이 통화의 모든 금액 또한 삭제됩니다.");
        languageManager.addLanguageEntry("currency_edit_cmd_help", "/통화 수정 <이름/이름복수형/마이너/마이너복수형/표식> <통화 이름> <새 값> - 통화 수정하기.");
        languageManager.addLanguageEntry("currency_help_cmd_help", "/통화 - 현재 명령어 도움말 보기");
        languageManager.addLanguageEntry("currency_info_cmd_help", "/통화 정보 <이름> - 통화에 대한 정보 표시하기.");
        languageManager.addLanguageEntry("default_currency_set", "%s{{DARK_GREEN}}(이)가 기본 통화로 설정되었습니다!");
        languageManager.addLanguageEntry("currency_deleted", "{{DARK_GREEN}}통화 삭제됨!");
        languageManager.addLanguageEntry("currency_modified", "{{DARK_GREEN}}통화 수정됨!");
        languageManager.addLanguageEntry("invalid_type", "{{DARK_RED}}올바르지 않은 종류!");
        languageManager.addLanguageEntry("currency_empty_value", "{{DARK_RED}}현재 통화 값을 공백으로 바꿀 수 없습니다 (Aka \\)");
        languageManager.addLanguageEntry("currency_help_title", "{{DARK_GREEN}} ======== 통화 명령어 ========");
        languageManager.addLanguageEntry("currency_info_name", "{{DARK_GREEN}}이름: {{WHITE}}%s");
        languageManager.addLanguageEntry("currency_info_name_plural", "{{DARK_GREEN}}이름 복수형: {{WHITE}}%s");
        languageManager.addLanguageEntry("currency_info_minor", "{{DARK_GREEN}}소수점: {{WHITE}}%s");
        languageManager.addLanguageEntry("currency_info_minor_plural", "{{DARK_GREEN}}소수점 복수형: {{WHITE}}%s");
        languageManager.addLanguageEntry("money_all_title", "{{DARK_GREEN}}금액: ");
        languageManager.addLanguageEntry("money_all_cmd_help", "/돈 모두 - 전 세계에서 소지한 금액 보기");
        languageManager.addLanguageEntry("money_balance_cmd_help", "/돈 금액 <플레이어 이름> - 플레이어의 금액 보기");
        languageManager.addLanguageEntry("money_create_cmd_help", "/돈 생성 <이름> - 계좌 만들기");
        languageManager.addLanguageEntry("money_delete_cmd_help", "/돈 삭제 <이름> - 계좌 삭제하기");
        languageManager.addLanguageEntry("money_give_cmd_help", "/돈 주기 <플레이어 이름> <수량> [통화] [세계] - 누군가에게 돈 주기");
        languageManager.addLanguageEntry("money_main_cmd_help", "/돈  - 소지한 금액 나열하기");
        languageManager.addLanguageEntry("money_help_cmd_help", "/돈 도움말 - 돈 도움말 보기");
        languageManager.addLanguageEntry("money_pay_cmd_help", "/돈 지불 <플레이어 이름> <수량> [통화] - 누군가에게 돈 보내기");
        languageManager.addLanguageEntry("money_set_cmd_help", "/돈 설정 <플레이어 이름> <수량> [통화] [세계] - 누군가의 금액 설정하기");
        languageManager.addLanguageEntry("money_take_cmd_help", "/돈 뺏기 <플레이어 이름> <수량> [통화] [세계] - 누군가의 금액 빼가기");
        languageManager.addLanguageEntry("money_top_cmd_help", "/돈 순위 <통화> [장] [세계] - 상위 목록 보기");
        languageManager.addLanguageEntry("money_create_success", "{{DARK_GREEN}} 계좌 생성됨!");
        languageManager.addLanguageEntry("money_delete_success", "{{DARK_GREEN}}그 계좌 {{WHITE}}%s {{DARK_GREEN}}은/는 삭제되었습니다!");
        languageManager.addLanguageEntry("money_give_received", "{{WHITE}}%s {{DARK_GREEN}}을/를 {{WHITE}}%s{{DARK_GREEN}}에게서 받았습니다");
        languageManager.addLanguageEntry("money_give_send", "{{WHITE}}%s {{DARK_GREEN}}을/를 {{WHITE}}%s{{DARK_GREEN}}에 주었습니다");
        languageManager.addLanguageEntry("money_help_title", "{{DARK_GREEN}} ======== 돈 명령어========");
        languageManager.addLanguageEntry("money_pay_sent", "{{WHITE}}%s {{DARK_GREEN}}을/를 {{WHITE}}%s{{DARK_GREEN}}에 보냈습니다");
        languageManager.addLanguageEntry("money_pay_received", "{{WHITE}}%s {{DARK_GREEN}}을/를 {{WHITE}}%s{{DARK_GREEN}}로부터 받았습니다");
        languageManager.addLanguageEntry("money_set", "{{WHITE}}%s {{DARK_GREEN}}금액을 {{WHITE}}%s{{DARK_GREEN}}에 설정했습니다");
        languageManager.addLanguageEntry("money_set_other", "{{DARK_GREEN}}당신의 돈은 {{WHITE}}%s {{DARK_GREEN}}로 설정되었습니다 {{WHITE}}%s{{DARK_GREEN}}님에 의해서");
        languageManager.addLanguageEntry("money_take", "{{WHITE}}%s {{DARK_GREEN}}을/를 {{WHITE}}%s {{DARK_GREEN}}에게서 가져갔습니다");
        languageManager.addLanguageEntry("money_take_other", "{{WHITE}}%s {{DARK_GREEN}}만큼 당신의 계좌에서 삭제되었습니다 {{WHITE}}%s{{DARK_GREEN}}님에 의해ㅔ서");
        languageManager.addLanguageEntry("player_not_exist", "{{DARK_RED}}그 플레이어는 존재하지 않습니다!");
        languageManager.addLanguageEntry("invalid_page", "{{DARK_RED}}올바르지 않은 장!");
        languageManager.addLanguageEntry("money_top_header", "{{DARK_GREEN}} 돈 순위 | {{WHITE}}%s {{DARK_GREEN}} 장 | {{WHITE}}%s {{DARK_GREEN}}세계");
        languageManager.addLanguageEntry("payday_create_cmd_help", "/지급일 생성 <이름> <주기> <임금/세금> <수량> [계좌] [통화 이름] [세계 이름] - 새 지급일을 만들기");
        languageManager.addLanguageEntry("payday_delete_cmd_help", "/지급일 삭제 <이름> - 지급일을 삭제하기.");
        languageManager.addLanguageEntry("payday_help_cmd_help", "/지급일 - 지급일 명령어 도움말을 보기");
        languageManager.addLanguageEntry("payday_info_cmd_help", "/지급일 정보 <지급일 이름> - 지급일에 대한 정보 보기.");
        languageManager.addLanguageEntry("payday_list_cmd_help", "/지급일 목록 - 모든 지급일 나열하기");
        languageManager.addLanguageEntry("payday_modify_cmd_help", "/지급일 수정 <이름> <Name/status/disabled/interval/amount/account/currency/World> <값> - 지급일 설정 수정하기.");
        languageManager.addLanguageEntry("payday_help_title", "{{DARK_GREEN}} ======== 지급일 명령어 ========");
        languageManager.addLanguageEntry("payday_already_exist", "{{DARK_RED}}그 이름의 지급일이 이미 있습니다!");
        languageManager.addLanguageEntry("invalid_interval", "{{DARK_RED}}올바르지 않은 주기!");
        languageManager.addLanguageEntry("payday_invalid_mode", "{{DARK_RED}}올바르지 않은 모드. 오직 임금 또는 세금이 지원됩니다!");
        languageManager.addLanguageEntry("payday_create_success", "{{DARK_GREEN}}지급일 추가됨! 이 지급일을 추가하길 원하는 플레이어에게 {{WHITE}}%s {{DARK_GREEN}}권한 노드를 추가하세요!");
        languageManager.addLanguageEntry("payday_not_found", "{{DARK_RED}}지급일 발견 못함!");
        languageManager.addLanguageEntry("error_occured", "{{DARK_GREEN}}오류 발생함. 콘솔을 봐서 오류를 확인하세요!");
        languageManager.addLanguageEntry("payday_removed", "{{DARK_GREEN}}지급일 제거됨!");
        languageManager.addLanguageEntry("payday_info_title", "{{DARK_GREEN}} ======== {{WHITE}}%s 정보 {{DARK_GREEN}}========");
        languageManager.addLanguageEntry("payday_info_type_wage", "{{DARK_GREEN}}종류: {{WHITE}}임금");
        languageManager.addLanguageEntry("payday_info_type_tax", "{{DARK_GREEN}}종류: {{WHITE}}세금");
        languageManager.addLanguageEntry("payday_info_account", "{{DARK_GREEN}}계좌: {{WHITE}}%s");
        languageManager.addLanguageEntry("payday_info_interval", "{{DARK_GREEN}}주기: {{WHITE}}%s");
        languageManager.addLanguageEntry("payday_info_amount", "{{DARK_GREEN}}수량: {{WHITE}}%s");
        languageManager.addLanguageEntry("payday_list_title", "{{DARK_GREEN}} ========= {{WHITE}} 지급일 수정 {{DARK_GREEN}}=========");
        languageManager.addLanguageEntry("invalid_edit_mode", "{{DARK_RED}}올바르지 않은 편집 모드.");
        languageManager.addLanguageEntry("world_changed", "{{DARK_GREEN}}세계 변경됨!");
        languageManager.addLanguageEntry("currency_changed", "{{DARK_GREEN}}통화 변경됨!");
        languageManager.addLanguageEntry("account_changed", "{{DARK_GREEN}}계좌 변경됨!");
        languageManager.addLanguageEntry("amount_changed", "{{DARK_GREEN}}수량 변경됨!");
        languageManager.addLanguageEntry("interval_changed", "{{DARK_GREEN}}주기 변경됨!");
        languageManager.addLanguageEntry("disabled_changed", "{{DARK_GREEN}}비활성화 변경됨!");
        languageManager.addLanguageEntry("status_changed", "{{DARK_GREEN}}상태 변경됨!");
        languageManager.addLanguageEntry("name_changed", "{{DARK_GREEN}}이름 변경됨!");
        languageManager.addLanguageEntry("invalid_interval", "{{DARK_RED}}올바르지 않은 주기! 초 단위의 숫자가 필요합니다! (예시: 60은 60초)");
        languageManager.addLanguageEntry("invalid_disabled", "{{DARK_RED}}올바르지 않은 비활성화 모드! 올바른 값: true/false");
        languageManager.addLanguageEntry("invalid_status", "{{DARK_RED}}올바르지 않은 상태! 올바른 값: 임금/세금");
        languageManager.addLanguageEntry("payday_with_name_already_exist", "{{DARK_RED}}이 이름의 지급날이 이미 존재합니다!");
        languageManager.addLanguageEntry("money_exchange_cmd_help", "/돈 교환 <현재 통화> <새 통회> <수량> -툥화 교환하기");
        languageManager.addLanguageEntry("no_exchange_rate", "{{WHITE}}%s {{DARK_RED}}의 금리를 {{WHITE}}%s{{DARK_RED}}로 바꿀 수 없습니다!");
        languageManager.addLanguageEntry("exchange_done", "{{WHITE}}%s %s {{DARK_GREEN}}을/를 {{WHITE}}%s %s{{DARK_RED}}로 전환 완료했습니다");
        languageManager.addLanguageEntry("currency_exchange_cmd_help", "/통화 교환 <교환받을 통화> <교환할 통화> <수량> - 통화 교환 금리 설정하기");
        languageManager.addLanguageEntry("currency_exchange_set", "통화 교환 설정됨!");
        languageManager.addLanguageEntry("world_group_manager_loaded", "세계 연합 관리자 불러와짐!");
        languageManager.addLanguageEntry("group_create_cmd_help", "/경제연합 생성 <이름> - 새 세계 연합을 만듭니다.");
        languageManager.addLanguageEntry("group_addworld_cmd_help", "/경제연합 세계추가 <연합 이름> <세계 이름> - 세계를 세계 연합에 추가합니다.");
        languageManager.addLanguageEntry("group_already_exist", "{{DARK_RED}}이 세계 연합은 이미 존재합니다!");
        languageManager.addLanguageEntry("group_created", "세계 연합 생성됨!");
        languageManager.addLanguageEntry("group_not_exist", "{{DARK_RED}}이 세계 연합은 존재하지 않습니다!");
        languageManager.addLanguageEntry("group_world_added", "세계를 연합에 추가했습니다!");
        languageManager.addLanguageEntry("world_already_in_group", "{{DARK_RED}}이 세계는 이미 연합 안에 있습니다! {{WHITE}}/경제연합 세계삭제 %s 로 지울 수 있습니다");
        languageManager.addLanguageEntry("group_delworld_cmd_help", "/경제연합 세계삭제 <세계 이름> - 연합에서 세계를 삭제합니다. 기본 연합으로 다시 돌아갑니다.");
        languageManager.addLanguageEntry("world_not_in_group", "{{DARK_RED}}이 세계는 연합에 있지 않습니다!");
        languageManager.addLanguageEntry("world_removed_from_group", "세계는 연합에서 제거되었습니다! 기본 연합으로 설정되었습니다.");
        languageManager.addLanguageEntry("loading_currency_manager", "통화 관리자 불러오는 중.");
        languageManager.addLanguageEntry("command_prefix", "{{DARK_GREEN}}[{{WHITE}}돈{{DARK_GREEN}}]{{WHITE}} ");
        languageManager.addLanguageEntry("group_help_title", "{{DARK_GREEN}} ======== 연합 명령어========");
        languageManager.addLanguageEntry("group_help_cmd_help", "/경제연합 - 경제연합 도움말 보기.");
        languageManager.addLanguageEntry("money_infinite_cmd_help", "/돈 무제한 <계좌 이름> - 계좌에 무제한 돈 모드를 넣습니다.");
        languageManager.addLanguageEntry("money_infinite_set_false", "그 계좌는 더 이상 무제한이 아닙니다!");
        languageManager.addLanguageEntry("money_infinite_set_true", "그 계좌는 이제 무제한입니다!");
        languageManager.addLanguageEntry("money_log_header", "{{DARK_GREEN}} 돈 기록 | {{WHITE}}%s {{DARK_GREEN}} 장 | {{WHITE}}%s {{DARK_GREEN}}계좌");
        languageManager.addLanguageEntry("money_log_cmd_help", "/돈 기록 <장> [계좌 이름] - 계좌 기록 보기");
        languageManager.addLanguageEntry("bank_list_cmd_help", "/은행 목록 - 접근 가능한 모든 은행 계좌 나열하기");
        languageManager.addLanguageEntry("bank_account_list", "은행 계좌 목록: %s");
        languageManager.addLanguageEntry("currency_rates_cmd_help", "/통화 금리 - 교환 가능한 모든 금리 보기.");
        languageManager.addLanguageEntry("rates_header", "{{DARK_GREEN}}[통화 금리]");
        languageManager.addLanguageEntry("bank_delete_cmd_help", "/은행 삭제 <이름> - 자신이 소유한 은행 계좌 삭제하기.");
        languageManager.addLanguageEntry("bank_delete_not_owner", "{{DARK_RED}}당신은 은행 소유자가 아닙니다!");
        languageManager.addLanguageEntry("currency_list_cmd_help", "/통화 목록 - 모든 통화 나열하기");
        languageManager.addLanguageEntry("currency_list_title", "{{DARK_GREEN}}====== {{WHITE}}통화들 {{DARK_GREEN}}======");
        languageManager.addLanguageEntry("invalid_time_log", "올바르지 않은 시간! 가능한 번호들이 되야합니다!");
        languageManager.addLanguageEntry("log_cleared", "당신이 말한 시간까지의 기록 표가 삭제됩니다!");
        languageManager.addLanguageEntry("craftconomy_clearlog_cmd_help", "/경제 기록청소 <날짜> - 제공한 값보다 오래된 기록 표 전체를 청소합니다");
        languageManager.addLanguageEntry("bank_ignoreacl_cmd_help", "/은행 acl무시 <계좌 이름>  - 계좌의 ACL 구조를 무시합니다.");
        languageManager.addLanguageEntry("account_is_ignoring_acl", "그 계좌는 이제 ACL가 무시됩니다!");
        languageManager.addLanguageEntry("account_is_not_ignoring_acl", "그 계좌는 이제 ACL를 따릅니다!");
        languageManager.addLanguageEntry("starting_database_convert", "MySQL로 데이터 전환 준비 중. 시간이 조금 소요됩니다.");
        languageManager.addLanguageEntry("convert_save_account", "계좌 전환중... (1/9)");
        languageManager.addLanguageEntry("convert_save_balance", "금액 전환중... (2/9)");
        languageManager.addLanguageEntry("convert_save_access", "은행 접근 전환중... (3/9)");
        languageManager.addLanguageEntry("convert_save_currency", "통화 전환중... (4/9)");
        languageManager.addLanguageEntry("convert_save_config", "설정 전환중... (5/9)");
        languageManager.addLanguageEntry("convert_save_payday", "지급날 전환중... (6/9)");
        languageManager.addLanguageEntry("convert_save_exchange", "교환 전환중... (7/9)");
        languageManager.addLanguageEntry("convert_save_worldgroup", "월드그룹 전환중... (8/9)");
        languageManager.addLanguageEntry("convert_save_log", "기록 전환중... (9/9)");
        languageManager.addLanguageEntry("convert_done", "전환 완료!");
        languageManager.addLanguageEntry("config_reload_help_cmd", "/경제 갱신 - 경제를 다시 불러오기.");
        languageManager.addLanguageEntry("craftconomy_reloaded", "경제가 다시 불러와졌습니다!");
    }

    /**
     * Initialize the configuration file
     */
    private void initializeConfig() {
        mainConfig.setValue("System.Setup", true);
        mainConfig.setValue("System.QuickSetup.Enable", false);
        mainConfig.setValue("System.QuickSetup.Currency.Name", "Dollar");
        mainConfig.setValue("System.QuickSetup.Currency.NamePlural", "Dollars");
        mainConfig.setValue("System.QuickSetup.Currency.Minor", "Coin");
        mainConfig.setValue("System.QuickSetup.Currency.MinorPlural", "Coins");
        mainConfig.setValue("System.QuickSetup.Currency.Sign", "$");
        mainConfig.setValue("System.QuickSetup.StartBalance", 100.0);
        mainConfig.setValue("System.QuickSetup.PriceBank", 200.0);
        mainConfig.setValue("System.QuickSetup.DisplayMode", "long");
        mainConfig.setValue("System.CheckNewVersion", true);
        mainConfig.setValue("System.Case-sentitive", false);
        mainConfig.setValue("System.CreateOnLogin", false);
        mainConfig.setValue("System.Logging.Enabled", false);
        mainConfig.setValue("System.Database.Type", "h2");
        mainConfig.setValue("System.Database.Address", "localhost");
        mainConfig.setValue("System.Database.Port", 3306);
        mainConfig.setValue("System.Database.Username", "root");
        mainConfig.setValue("System.Database.Password", "");
        mainConfig.setValue("System.Database.Db", "craftconomy");
        mainConfig.setValue("System.Database.Prefix", "cc3_");
        mainConfig.setValue("System.Database.ConvertFromSQLite", false);
    }

    /**
     * Run a database update.
     */
    private void updateDatabase() {
        if (getMainConfig().getInt("Database.dbVersion") == 0) {
            alertOldDbVersion(0, 1);
            //We first check if we have the DB version in the database. If we do, we have a old layout in our hands
            String value = getStorageHandler().getStorageEngine().getConfigEntry("dbVersion");
            if (value != null) {
                //We have a old database, do the whole conversion
                try {
                    new OldFormatConverter().run();
                    getMainConfig().setValue("Database.dbVersion", 1);
                    sendConsoleMessage(Level.INFO, "Updated to Revision 1!");

                } catch (SQLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            } else {
                getMainConfig().setValue("Database.dbVersion", 1);
                sendConsoleMessage(Level.INFO, "Updated to Revision 1!");
            }
        } else if (getMainConfig().getInt("Database.dbVersion") == -1) {
            alertOldDbVersion(-1,1);
            try {
                    new OldFormatConverter().step2();
                    getMainConfig().setValue("Database.dbVersion", 1);
                    sendConsoleMessage(Level.INFO, "Updated to Revision 1!");

                } catch (SQLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ParseException e) {
                    e.printStackTrace();
                }     
        }
    }

    /**
     * Alert in the console of a database update.
     *
     * @param currentVersion The current version
     * @param newVersion     The database update version
     */
    private void alertOldDbVersion(int currentVersion, int newVersion) {
        Common.getInstance().sendConsoleMessage(Level.INFO, "Your database is out of date! (Version " + currentVersion + "). Updating it to Revision " + newVersion + ".");
    }


}
