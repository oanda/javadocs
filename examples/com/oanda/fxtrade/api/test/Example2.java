package com.oanda.fxtrade.api.test;

import java.text.DecimalFormat;
import java.util.Collection;
import java.util.Observable;
import java.util.Observer;
import java.util.Vector;

import com.oanda.fxtrade.api.API;
import com.oanda.fxtrade.api.Account;
import com.oanda.fxtrade.api.AccountException;
import com.oanda.fxtrade.api.FXClient;
import com.oanda.fxtrade.api.FXHistoryPoint;
import com.oanda.fxtrade.api.FXPair;
import com.oanda.fxtrade.api.FXPairException;
import com.oanda.fxtrade.api.FXTick;
import com.oanda.fxtrade.api.Instrument;
import com.oanda.fxtrade.api.LimitOrder;
import com.oanda.fxtrade.api.MarketOrder;
import com.oanda.fxtrade.api.OAException;
import com.oanda.fxtrade.api.RateTable;
import com.oanda.fxtrade.api.RateTableException;
import com.oanda.fxtrade.api.SessionException;
import com.oanda.fxtrade.api.TrailingStop;
import com.oanda.fxtrade.api.Transaction;
import com.oanda.fxtrade.api.User;

public final class Example2 implements Observer {

	public static final String VERSION = "1.20";

	// For keyboard input
	public static final EasyIn keyboard = new EasyIn();

	// the FXClient object will perform all interactions with the OANDA FXGame/FXTrade server
	FXClient fxclient;

	// The current FXTrade user
	User user;
	String username;
	String password;

	// The current account
	Account account;

	// The current rates table
	RateTable rateTable;

	boolean show_debug_msg = false;

public void caseChangeAccount() {

	if (!fxclient.isLoggedIn()) {
		return;
	}

	// fetch and print out a list of accounts to the screen
	Vector<? extends Account> accounts = caseGetAccounts();

	// get user selection
	System.out.println("\nPlease enter the account number or place in list [0]: ");
	int accountNum = keyboard.readInt();

	// return if the user presses enter or 0
	if (accountNum == 0) {
		return;
	}

	if (accounts != null) {
		if (accountNum <= accounts.size()) {
			// if the input number is small enough assume the user is picking from the list
			account = (Account) accounts.elementAt(accountNum - 1);
			accountNum = account.getAccountId();
			System.out.println("Example: now trading on account " + accountNum);
		}
		else {
			try {
				// select the account based on account number
				Account temp = user.getAccountWithId(accountNum);
				account = temp;
				System.out.println("Example: now trading on account " + accountNum);
			}
			catch(AccountException ae) {
				System.out.println(ae.getMessage());
			}
		}
	}



}
public void caseGetAccountData() {

	if (!fxclient.isLoggedIn()) {
		return;
	}

	// print out some useful account information
	// these are the same calculations as the FXTrade platform
	try {
		System.out.println("Balance:            " + account.getBalance());
		System.out.println("Unrealized P/L:     " + account.getUnrealizedPL());
		System.out.println("Net Asset Value:    " + (account.getBalance() + account.getUnrealizedPL()));
		System.out.println("Realized P/L:       " + account.getRealizedPL());
		System.out.println("Margin Used:        " + account.getMarginUsed());
		System.out.println("Margin Avalailable: " + account.getMarginAvailable());
		System.out.println("Margin Percent:     " + ((account.getPositionValue() != 0) ? "" + (account.getBalance() + account.getUnrealizedPL()) / account.getPositionValue() * 100 : "n/a"));
		System.out.println("Position Value:     " + account.getPositionValue());
		System.out.println("Position * " + account.getMarginCallRate() * 100 + "%:    " + account.getPositionValue() * account.getMarginCallRate());
	}
	catch (AccountException ae) {
		System.err.println("Example: caught: " + ae);
	}
	catch (Exception e) {
		System.err.println("Example: caught: " + e);
	}
}
public Vector<? extends Account> caseGetAccounts() {

	if (!fxclient.isLoggedIn()) {
		return null;
	}

	// request the user's list of accounts
	Vector<? extends Account> accounts = user.getAccounts();
	if (accounts != null) {
		for (int i = 0; i < accounts.size(); i++) {
			System.out.println((i + 1) + ": " + accounts.elementAt(i));
		}
	}
	return accounts;
}
public Vector<? extends Transaction> caseGetActivityLog() {

	if (!fxclient.isLoggedIn()) {
		return null;
	}

	// request the activity log for the current account
	// a vector of up to 300 Transaction objects is returned
	Vector<? extends Transaction> log = null;
	try {
		log = account.getTransactions();
		if (log != null) {
			for (int i = 0; i < log.size(); i++) {
				System.out.println((i + 1) + ": " + log.elementAt(i));
			}
		}
	}
	catch (AccountException ae) {
		System.out.println("caseGetActivityLog(): caught: " + ae);
	}
	return log;
}
public Vector<? extends FXHistoryPoint> caseGetHistory() {

	if (!fxclient.isLoggedIn()) {
		return null;
	}

	// get the currency pair
	System.out.println("Pair [EUR/USD]: ");
	String pairInput = keyboard.readString().toUpperCase();
	FXPair pair = null;
	if (pairInput.equals("")) {
		pair = API.createFXPair("EUR/USD"); // default to EUR/USD
	}
	else {
		pair = API.createFXPair(pairInput);
	}

	// get the interval
	System.out.println("Please enter the interval in seconds [5]: ");
	// 5 = 5 sec
	// 10 = 10 sec
	// 30 = 30 sec
	// 60 = 1 min
	// 300 = 5 min
	// 1800 = 30 min
	// 10800 = 3 hour
	// 86400  = 1 day
	long interval = keyboard.readInt();
	interval *= 1000;  // convert to milliseconds
	if (interval == 0) {
		interval = 5000; // default to 5 seconds
	}

	// get the number of ticks
	System.out.println("Please enter the number of ticks (max 500) [100]: ");
	int ticks = keyboard.readInt();
	if (ticks == 0) {
		ticks = 100; // default to 100 ticks
	}

	// make the history request
	// the FXTrade object returns as many FXHistoryPoints as possible up to the number requested,
	System.out.println("Example: rate history request...");
	Vector<? extends FXHistoryPoint> history = null;
	try {
		//history = fxclient.getHistory(pair, interval, ticks); // this call has been deprecated
		history = rateTable.getHistory(pair, interval, ticks);
	}
	catch (OAException se) {
		System.err.println("Example: rate history request failed: " + se);
	}

	if (history != null && history.size() > 0) {
		System.out.println("Time|MaxBid|MaxAsk|OpenBid|OpenAsk|CloseBid|CloseAsk|MinBid|MinAsk");
		System.out.println("------------------------------------------------------------------");
		int i = 0;
		for (; i < history.size() - 1; i++) {
			System.out.println(history.elementAt(i));
		}
		// The last tick is a 'state tick' that represents the current, incomplete tick
		System.out.println(history.elementAt(i) + " (Current State)");
		System.out.println("Obtained " + history.size() + " points.");
	}
	return history;
}
public Vector<? extends LimitOrder> caseGetOpenOrders() {

	if (!fxclient.isLoggedIn()) {
		return null;
	}

	// request the the current account's LimitOrders
	Vector<? extends LimitOrder> orders = null;
	try {
		orders = account.getOrders();
		if (orders != null) {
			for (int i = 0; i < orders.size(); i++) {
				System.out.println((i + 1) + ": " + orders.elementAt(i));
			}
		}
	}
	catch (AccountException ae) {
		System.out.println("caseGetOpenOrders(): caught: " + ae);
	}
	return orders;
}
public Vector<? extends MarketOrder> caseGetOpenTrades() {

	if (!fxclient.isLoggedIn()) {
		return null;
	}

	// request the the current account's MarketOrders
	Vector<? extends MarketOrder> trades = null;
	try {
		trades = account.getTrades();
		if (trades != null) {
			for (int i = 0; i < trades.size(); i++) {
				System.out.println((i + 1) + ": " + trades.elementAt(i));
			}
		}
	}
	catch (AccountException ae) {
		System.out.println("caseGetOpenTrades(): caught: " + ae);
	}
	return trades;
}

public static final DecimalFormat commaFormatter = new DecimalFormat("###,###,###");
public void caseGetRates() {

	if (!fxclient.isLoggedIn()) {
		return;
	}

	try {
		rateTable = fxclient.getRateTable();
		if (rateTable != null) {
			Collection<FXPair> pairs = rateTable.getAllSymbols();

			System.out.println("\nPlease enter a number of units [0]: ");
			long units = keyboard.readInt();

			System.out.println(" Pair  |   Time   |  Bid  |  Ask  | Max Units");
			System.out.println("---------------------------------------------");
			for(FXPair pair : pairs) {
				Instrument instrument = rateTable.getInstrument(pair.getPair());

				if(instrument.getStatus() == 0) { //active, not halted
					FXTick rate = rateTable.getRateForUnits(pair, units);
					if (rate != null) {
					    System.out.println(rate + " " + commaFormatter.format(rate.getMaxUnits()));
					}
				}
			}
//			System.out.println("AUD/USD" + " " + rateTable.getRate(API.createFXPair("AUD/USD")));
//			System.out.println("EUR/CHF" + " " + rateTable.getRate(API.createFXPair("EUR/CHF")));
//			System.out.println("EUR/GBP" + " " + rateTable.getRate(API.createFXPair("EUR/GBP")));
//			System.out.println("EUR/JPY" + " " + rateTable.getRate(API.createFXPair("EUR/JPY")));
//			System.out.println("EUR/USD" + " " + rateTable.getRate(API.createFXPair("EUR/USD")));
//			System.out.println("GBP/CHF" + " " + rateTable.getRate(API.createFXPair("GBP/CHF")));
//			System.out.println("GBP/JPY" + " " + rateTable.getRate(API.createFXPair("GBP/JPY")));
//			System.out.println("GBP/USD" + " " + rateTable.getRate(API.createFXPair("GBP/USD")));
//			System.out.println("USD/CAD" + " " + rateTable.getRate(API.createFXPair("USD/CAD")));
//			System.out.println("USD/CHF" + " " + rateTable.getRate(API.createFXPair("USD/CHF")));
//			System.out.println("USD/JPY" + " " + rateTable.getRate(API.createFXPair("USD/JPY")));
//			System.out.println("AUD/CAD" + " " + rateTable.getRate(API.createFXPair("AUD/CAD")));
		}
	}
	catch (SessionException se) {
		System.out.println("caseGetRates(): caught: " + se);
	}
	catch (RateTableException re) {
		System.out.println("caseGetRates(): caught: " + re);
	}

}
public void caseLimitCancel() {
	try {
		if (!fxclient.isLoggedIn()) {
			return;
		}

		// fetch and print out a list of the user's open orders
		Vector<? extends LimitOrder> orders = caseGetOpenOrders();

		// get user selection
		System.out.println("\nPlease enter a ticket number or place in list [0]: ");
		int ticket = keyboard.readInt();

		// return if the user presses enter or 0
		if (ticket == 0) {
			return;
		}

		// if the input number is small enough assume the user is picking from the list
		LimitOrder limitOrder = null;
		if (orders != null) {
			if (ticket <= orders.size()) {
				limitOrder = (LimitOrder) orders.elementAt(ticket - 1);
				ticket = limitOrder.getTransactionNumber();
			}
			else {
				limitOrder = account.getOrderWithId(ticket);
			}
		}

		// request the limit order cancellation
		System.out.println("Example: cancelling limit order " + ticket + "...");
		account.close(limitOrder);
		System.out.println("Example: limit order " + ticket + " cancelled successfully");
	}
	catch (SessionException se) {
		System.err.println("Example: limit order cancellation failed: " + se);
	}
	catch (Exception e) {
		System.err.println("Example: limit order cancellation failed: " + e);
	}
}
public void caseLimitModify() {
	try {
		if (!fxclient.isLoggedIn()) {
			return;
		}

		// fetch and print out a list of the user's open orders
		Vector<? extends LimitOrder> orders = caseGetOpenOrders();

		// get user selection
		System.out.println("\nPlease enter a ticket number or place in list [0]: ");
		int ticket = keyboard.readInt();

		// return if the user presses enter or 0
		if (ticket == 0) {
			return;
		}

		// if the input number is small enough assume the user is picking from the list
		LimitOrder limitOrder = null;
		if (orders != null) {
			if (ticket <= orders.size()) {
				limitOrder = (LimitOrder) orders.elementAt(ticket - 1);
				ticket = limitOrder.getTransactionNumber();
			}
			else {
				limitOrder = account.getOrderWithId(ticket);
			}
		}

		// bail if the order wasn't found
		if (limitOrder == null) {
			System.err.println("Example: unknown order: " + ticket);
			return;
		}

		// get the revised inputs, defaulting to the old values if the
		// user presses enter or 0.

		System.out.println("Units [" + Math.abs(limitOrder.getUnits()) + "]: ");
		int tempUnits = keyboard.readInt();
		if (tempUnits != 0) {
			limitOrder.setUnits(tempUnits);
		}
		System.out.println("Quote [" + limitOrder.getPrice() + "]: ");
		double tempQuote = keyboard.readDouble();
		if (tempQuote != 0) {
			limitOrder.setPrice(tempQuote);
		}
		System.out.println("Lower Bound [" + limitOrder.getLowPriceLimit() + "]: ");
		double tempLB = keyboard.readDouble();
		if (tempLB != 0) {
			limitOrder.setLowPriceLimit(tempLB);
		}
		System.out.println("Upper Bound [" + limitOrder.getHighPriceLimit() + "]: ");
		double tempUB = keyboard.readDouble();
		if (tempUB != 0) {
			limitOrder.setHighPriceLimit(tempUB);
		}
		System.out.println("Stop Loss [" + limitOrder.getStopLoss().getPrice() + "]: ");
		double tempSL = keyboard.readDouble();
		if (tempSL != 0) {
			limitOrder.setStopLoss(API.createStopLossOrder(tempSL));
		}
		System.out.println("Take Profit [" + limitOrder.getTakeProfit().getPrice() + "]: ");
		double tempTP = keyboard.readDouble();
		if (tempTP != 0) {
			limitOrder.setTakeProfit(API.createTakeProfitOrder(tempTP));
		}

		// order expiry is expressed as the timestamp when the order is to be cancelled,
		// so we'll work around that by getting the number of hours the user wants the order
		// to exist and then create an expiry timestamp
		double hours = (double) (limitOrder.getExpiry() - fxclient.getServerTime()) / (double) (60 * 60);
		System.out.println("Duration (hours) [" + hours + "]: ");
		double tempHours = keyboard.readDouble();
		if (tempHours != 0) {
			limitOrder.setExpiry((int) (fxclient.getServerTime() + tempHours * 60 * 60));
		}

		// request the limit order modification
		System.out.println("Example: modifying limit order " + ticket + "...");
		account.modify(limitOrder);
		System.out.println("Example: limit order " + ticket + " modified successfully");
	}
	catch (SessionException se) {
		System.err.println("Example: limit order modification failed: " + se);
	}
	catch (Exception e) {
		System.err.println("Example: limit order modification failed: " + e);
	}
}
public void caseLimitOrder() {
	try {
		if (!fxclient.isLoggedIn()) {
			return;
		}

		// fetch and print the current rate table
		caseGetRates();

		// create a base LimitOrder object
		LimitOrder limitOrder = API.createLimitOrder();

		// get the pair
		System.out.println("Pair [EUR/USD]: ");
		String pairInput = keyboard.readString().toUpperCase();
		if (pairInput.equals("")) {
			limitOrder.setPair(API.createFXPair("EUR/USD")); // default to EUR/USD
		}
		else {
			limitOrder.setPair(API.createFXPair(pairInput));
		}

		// get the number of units
		System.out.println("Number of units [100]: ");
		limitOrder.setUnits(Math.abs(keyboard.readInt()));
		if (limitOrder.getUnits() == 0) {
			limitOrder.setUnits(100); // default to 100
		}

		// find out if it's a buy or a sell
		System.out.println("(B)uy or (S)ell [B]: ");
		String temp = keyboard.readString();
		boolean buy = true;
		if (!temp.equals("")) {
			buy = temp.toUpperCase().startsWith("B"); // default to buy
		}
		// units are negative for a sell
		if (!buy) {
			limitOrder.setUnits(-1 * limitOrder.getUnits());
		}

		// figure out the current price for this pair
		double tempRate;
		FXTick tick = null;
		tick = rateTable.getRate(limitOrder.getPair());
		if (buy) {
			tempRate = tick.getAsk();
		}
		else {
			tempRate = tick.getBid();
		}

		// get the requested price
		System.out.println("Price [" + tempRate + "]:");
		limitOrder.setPrice(keyboard.readDouble());
		if (limitOrder.getPrice() == 0) {
			limitOrder.setPrice(tempRate); // default to the current price
		}

		// get the expiry
		System.out.println("Duration (in hours) [24]: ");
		long tempDuration = keyboard.readInt();
		if (tempDuration == 0) {
			tempDuration = 24; // default to 24 hours
		}
		// order expiry is expressed as the timestamp when
		// the order is to be cancelled.  we'll work around that by
		// getting the number of hours the user wants the order to exist
		// for and then create an expiry timestamp
		tempDuration *= 60 * 60;
		tempDuration += fxclient.getServerTime();
		limitOrder.setExpiry(tempDuration);

		//// to simplify the example, these are left as an exercise to the programmer...
		// stop loss
		// take profit
		// upper bound
		// lower bound

		System.out.println("Example: submitting limit order...");
		// submit the limit order request
		account.execute(limitOrder);
		System.out.println("Example: limit order entered successfully");
	}
	catch (SessionException se) {
		System.err.println("Example: limit order execution failed: " + se);
	}
	catch (Exception e) {
		System.err.println("Example: limit order execution failed: " + e);
	}
}
public void caseMarketClose() {
	try {
		if (!fxclient.isLoggedIn()) {
			return;
		}

		// fetch and print a list of the users open trades
		Vector<? extends MarketOrder> trades = caseGetOpenTrades();

		// get user selection
		System.out.println("\nPlease enter a ticket number or place in list [0]: ");
		int ticket = keyboard.readInt();

		// return if the user presses enter or 0
		if (ticket == 0) {
			return;
		}

		// if the input number is small enough assume the user is picking from the list
		MarketOrder marketOrder = null;
		if (trades != null) {
			if (ticket <= trades.size()) {
				marketOrder = (MarketOrder) trades.elementAt(ticket - 1);
				ticket = marketOrder.getTransactionNumber();
			}
			else {
				marketOrder = account.getTradeWithId(ticket);
			}
		}
		System.out.println("Example: closing market order " + ticket + "...");

		// submit the market order close request
		account.close(marketOrder);
		System.out.println("Example: market order " + ticket + " closed successfully");

		if (show_debug_msg) {
			// display resulting close transaction response
			Transaction resultOfGetCloseTrans = account.getTransactionWithId(marketOrder.getClose().getTransactionNumber());
			System.out.println("Example: close response transaction: " + resultOfGetCloseTrans);
		}
	}
	catch (SessionException se) {
		System.err.println("Example: market order close failed: " + se);
	}
	catch (Exception e) {
		System.err.println("Example: market order close failed: " + e);
	}
}
public void caseMarketModify() {
	try {
		if (!fxclient.isLoggedIn()) {
			return;
		}

		// fetch and print a list of the users open trades
		Vector<? extends MarketOrder> trades = caseGetOpenTrades();

		// get user selection
		System.out.println("\nPlease enter a ticket number or place in list [0]: ");
		int ticket = keyboard.readInt();

		// return if the user presses enter or 0
		if (ticket == 0) {
			return;
		}

		// if the input number is small enough assume the user is picking from the list
		MarketOrder marketOrder = null;
		if (trades != null) {
			if (ticket <= trades.size()) {
				marketOrder = (MarketOrder) trades.elementAt(ticket - 1);
				ticket = marketOrder.getTransactionNumber();
			}
			else {
				marketOrder = account.getTradeWithId(ticket);
			}
		}

		// bail if the trade wasn't found
		if (marketOrder == null) {
			System.err.println("Example: unknown trade: " + ticket);
			return;
		}

		// get the revised inputs, defaulting to the old values if the user presses enter or 0.
		// NOTE: this code will not work if the user actually *wants* to set the stoploss to 0.0
		System.out.println("Stop Loss [" + marketOrder.getStopLoss().getPrice() + "]: ");
		double tempSL = keyboard.readDouble();
		if (tempSL != 0) {
			marketOrder.setStopLoss(API.createStopLossOrder(tempSL));
		}

		// get the revised inputs, defaulting to the old values if the user presses enter or 0.
		// NOTE: this code will not work if the user actually *wants* to set the takeprofit to 0.0
		System.out.println("Take Profit [" + marketOrder.getTakeProfit().getPrice() + "]: ");
		double tempTP = keyboard.readDouble();
		if (tempTP != 0) {
			marketOrder.setTakeProfit(API.createTakeProfitOrder(tempTP));
		}

		// NOTE: this code will not work if the user actually *wants* to set the trailing stop to 0.0
        System.out.println("Trailing Stop Loss [" + marketOrder.getTrailingStopLoss() + "]: ");
        double tempTS = keyboard.readDouble();
        if (tempTS != 0) {
            marketOrder.setTrailingStopLoss(tempTS);
        }

		System.out.println("Example: modifying market order " + ticket + "...");
		// request the market order modification
		account.modify(marketOrder);
		System.out.println("Example: market order " + ticket + " modified successfully");
	}
	catch (SessionException se) {
		System.err.println("Example: market order modification failed: " + se);
	}
	catch (Exception e) {
		System.err.println("Example: market order modification failed: " + e);
	}
}
public void caseMarketOrder() {

	if (!fxclient.isLoggedIn()) {
		return;
	}

	// create a base MarketOrder object
	MarketOrder marketOrder = API.createMarketOrder();

	// get the pair
	System.out.println("Pair [EUR/USD]: ");
	String pairInput = keyboard.readString().toUpperCase();
	if (pairInput.equals("")) {
		marketOrder.setPair(API.createFXPair("EUR/USD")); // default to EUR/USD
	}
	else {
		marketOrder.setPair(API.createFXPair(pairInput));
	}

	// get the number of units
	System.out.println("Number of units [100]: ");
	marketOrder.setUnits(Math.abs(keyboard.readInt()));
	if (marketOrder.getUnits() == 0) {
		marketOrder.setUnits(100); // default to 100
	}

	// find out if it's a buy or a sell
	System.out.println("(B)uy or (S)ell [B]: ");
	String temp = keyboard.readString();
	boolean buy = true;
	if (!temp.equals("")) {
		buy = temp.toUpperCase().startsWith("B");  // default to buy
	}
	// units are negative for a sell
	if (!buy) {
		marketOrder.setUnits(-1 * marketOrder.getUnits());
	}

	System.out.println("Trailing Stop [0.0 PIP]: ");
	double tsValue = keyboard.readDouble();
	marketOrder.setTrailingStopLoss(tsValue);

	//// to simplify the example, these are left as an exercise to the programmer...
	// stop loss
	// take profit
	// upper bound
	// lower bound

	System.out.println("Example: submitting market order...");
	try {
		// submit the market order request
		account.execute(marketOrder);
		System.out.println("Example: market order entered successfully");
	}
	catch (OAException se) {
		System.err.println("Example: market order execution failed: " + se);
	}
}
public void caseGetTrailingStop() {
    try {
        if (!fxclient.isLoggedIn()) {
            return;
        }

        // fetch and print a list of the users open trades
        Vector<? extends MarketOrder> trades = caseGetOpenTrades();

        // get user selection
        System.out.println("\nPlease enter a ticket number or place in list [0]: ");
        int ticket = keyboard.readInt();

        // return if the user presses enter or 0
        if (ticket == 0) {
            return;
        }

        // if the input number is small enough assume the user is picking from the list
        MarketOrder marketOrder = null;
        if (trades != null) {
            if (ticket <= trades.size()) {
                marketOrder = (MarketOrder) trades.elementAt(ticket - 1);
                ticket = marketOrder.getTransactionNumber();
            }
            else {
                marketOrder = account.getTradeWithId(ticket);
            }
        }

        // bail if the trade wasn't found
        if (marketOrder == null) {
            System.err.println("Example: unknown trade: " + ticket);
            return;
        }

        TrailingStop ts = account.getTrailingStop(marketOrder);
        if ( ts == null ) {
            System.out.println( "No Trailing Stop for trade " + marketOrder.getTransactionNumber() );
        }
        else {
            System.out.println(ts);

            FXTick rate = rateTable.getRate(ts.getPair());

            Instrument instrument = rateTable.getInstrument(ts.getPair().getPair());
            double PIP = Math.pow(10, instrument.getPipettes() - instrument.getPrecision());
            double distanceAsPrice;
            double distance;
            distanceAsPrice = Math.abs( ts.getCurrentValue() - (ts.isBuy() ? rate.getBid() : rate.getAsk()) );
            distance = distanceAsPrice/PIP;
            System.out.println( "PIP Distance: " + new DecimalFormat("###.#").format(distance) );
        }

    }
    catch (Exception e) {
        System.err.println("Example: get trailing stop failed: " + e);
    }
}

public void caseViewLadderedPrices() {
    if (!fxclient.isLoggedIn()) {
        return;
    }

    // get the currency pair
    System.out.println("Pair [EUR/USD]: ");
    String pairInput = keyboard.readString().toUpperCase();
    FXPair pair = null;
    if (pairInput.equals("")) {
        pair = API.createFXPair("EUR/USD"); // default to EUR/USD
    }
    else {
        pair = API.createFXPair(pairInput);
    }

    try {
        System.out.println(" Pair  |   Time   |  Bid  |  Ask  | Max Units");
        System.out.println("---------------------------------------------");
        Collection<FXTick> ticks = rateTable.getRatesForAllUnits(pair);
        for(FXTick tick : ticks) {
            System.out.println(tick + " " + commaFormatter.format(tick.getMaxUnits()));
        }
    }
    catch (Exception e) {
        System.err.println("Example: limit order cancellation failed: " + e);
    }
}

public void caseToggleDebugMsg() {
	if (show_debug_msg) {
		show_debug_msg = false;
		System.out.println("Example: debug message turned off.");
	} else {
		show_debug_msg = true;
		System.out.println("Example: debug message turned on.");
	}
}
/*************************************************************************/
/* INIT()                                                                */
/*************************************************************************/
private void init(String username, String password) {

	try {
        com.oanda.fxtrade.api.Configuration.setVersion("2.3.9");

		System.out.println("Example: creating FXClient object...");

		fxclient = API.createFXGame();
		//fxclient.setTimeout(10);

		/*
		 * Set this object to be an Observer of the FXClient
		 * to be notified of connections/disconnections/updates from the server
		 */
		fxclient.addObserver(this);
//		update(fxclient,FXClient.CONNECTED);
		/*
		 * connect and login to the server
		 *
		 */
		System.out.println("Example: logging in...");
		fxclient.setProxy(false);
		fxclient.setWithRateThread(true);
		fxclient.login(username, password, "Example2 Test");

		// save the password in case a reconnection is needed later
		this.username = username;
		this.password = password;

		// launch the menu interface
		startMenuThread();
	}
	catch (OAException oa) {
		System.out.println("Example: caught: " + oa);
	}
}
/*************************************************************************/
/* MAIN()                                                                */
/*************************************************************************/
public static void main(String[] args) throws Exception {

	// basic initialization
	Example2 self = new Example2();

	// get the username and password from the command line
	if (args.length == 2) {
		String username = args[0];
		String password = args[1];
		self.init(username, password);
	}
	else {
		System.out.println("\nUsage: java " + Example2.class.getName() + " <username> <password>\n");
		System.exit(1);
	}
}
private void startMenuThread() {
	Thread menuThread = new Thread("Example.menuThread") {
		public void run() {

			System.out.println("Example: printing menu for the first time...");

			// Menu information to the printed on the screen
			String MENU =                                   //
				"\n\nOANDA FX Client Test v"+VERSION+"\n" + //
				"--------------------------\n" +            //
				"1.  List Accounts\n" +                     //
				"2.  Switch Accounts\n" +                   //
				"\n" +                                      //
				"3.  Print Account Data\n" +                //
				"4.  List Open Trades\n" +                  //
				"5.  List Open Orders\n" +                  //
				"6.  List Activity Log\n" +                 //
				"\n" +                                      //
				"7.  Print Rate Table\n" +                  //
				"\n" +                                      //
				"8.  Execute Market Order\n" +              //
				"9.  Modify Market Order\n" +               //
				"10. Close Market Order\n" +                //
				"\n" +                                      //
				"11. Execute Limit Order\n" +               //
				"12. Modify Limit Order\n" +                //
				"13. Cancel Limit Order\n" +                //
				"\n" +                                      //
				"14. Fetch History\n" +                     //
				"15. Get Trailing Stop\n" +                 //
				"\n" +                                      //
				"16. View Laddered Prices\n" +              //
				"\n" +                                      //
				"0.  Show Menu\n" +                         //
				"88. Toggle Debug Message\n" +				//
				"99. Quit"                                  //
			;
			System.out.println(MENU);

			// main control loop
			// get the user's selection and call the appropriate method
			do {
				System.out.println("\nPlease Choose an option from the menu:");
				System.out.print("> ");
				try {
					int selection = keyboard.readInt();
					switch (selection) {
						case 0 :
							System.out.println(MENU);
							break;
						case 1 :
							caseGetAccounts();
							break;
						case 2 :
							caseChangeAccount();
							break;
						case 3 :
							caseGetAccountData();
							break;
						case 4 :
							caseGetOpenTrades();
							break;
						case 5 :
							caseGetOpenOrders();
							break;
						case 6 :
							caseGetActivityLog();
							break;
						case 7 :
							caseGetRates();
							break;
						case 8 :
							caseMarketOrder();
							break;
						case 9 :
							caseMarketModify();
							break;
						case 10 :
							caseMarketClose();
							break;
						case 11:
							caseLimitOrder();
							break;
						case 12 :
							caseLimitModify();
							break;
						case 13 :
							caseLimitCancel();
							break;
						case 14 :
							caseGetHistory();
							break;
						case 15 :
						    caseGetTrailingStop();
						    break;
						case 16 :
						    caseViewLadderedPrices();
						    break;
						case 88 :
							caseToggleDebugMsg();
							break;
						case 99 :
							System.out.println("Exiting Program");
							System.exit(0);
							break;
						default :
							System.out.println("Please enter a valid number");
							break;
					}
				}
				catch (java.util.NoSuchElementException nsee) {
					System.out.println("Please enter a valid number");
				}
				catch (FXPairException fxpe) {
					System.out.println("Please enter a valid instrument (XXX/YYY)");
				}
				catch (NumberFormatException nfe) {
					System.out.println("Please enter a valid number");
				}
				catch (Exception e) {
					System.err.println("Caught: " + e);
					System.err.println("Exiting Program");
					System.exit(1);
				}
			}
			while (true);
		} // END RUN()
	}; // END MENUTHREAD()

	menuThread.start();
}
/*
 * Currently, the FXClient class will only notify it's Observers
 * of connections and disconnections from the server
 */
public void update(Observable source, Object status) {
	try {
		if (source == fxclient) {
			// connected to server, update User, Account and RateTable
			if (status.equals(FXClient.CONNECTED)) {
				System.out.println("Example: setting user...");
				user = fxclient.getUser();
				//
				System.out.println("Example: setting account...");
				account = (Account) user.getAccounts().elementAt(0);
				//
				System.out.println("Example: fetching rate table...");
				rateTable = fxclient.getRateTable();
			}

			// disconnected from server, attempt reconnection
			else if (status.equals(FXClient.DISCONNECTED)) {
				System.out.println("Example: disconnection detected.  attempting to reconnect...");
				fxclient.login(username, password, "Example2 Test");
			}

			//
			// WARNING: this feature is not ready yet.  use at your own risk.
			//
			//// asynchronous update: stop loss, take profit, market/limit order, margin call, etc
			//else if (status == FXClient.UPDATE_TRADES) {
				//System.out.println("Example: detected asynchronous trades update");
			//}
			//else if (status == FXClient.UPDATE_ORDERS) {
				//System.out.println("Example: detected asynchronous orders update");
			//}
			//else if (status == FXClient.UPDATE_TRANSACTIONS) {
				//System.out.println("Example: detected asynchronous activity update");
			//}
			//else if (status == FXClient.UPDATE) {
				//System.out.println("Example: detected asynchronous update");
			//}
			// just in case we get something strange
			//else {
				//System.out.println("Example: unknown notification type: " + status);
			//}
		}
	}
	catch (OAException oa) {
		System.out.println("Example: caught: " + oa);
	}
}
}
