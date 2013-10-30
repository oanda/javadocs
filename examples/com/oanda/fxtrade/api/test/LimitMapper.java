package com.oanda.fxtrade.api.test;

import java.util.Vector;

import com.oanda.fxtrade.api.API;
import com.oanda.fxtrade.api.Account;
import com.oanda.fxtrade.api.AccountException;
import com.oanda.fxtrade.api.FXAccountEvent;
import com.oanda.fxtrade.api.FXAccountEventInfo;
import com.oanda.fxtrade.api.FXClient;
import com.oanda.fxtrade.api.FXEventInfo;
import com.oanda.fxtrade.api.FXEventManager;
import com.oanda.fxtrade.api.InvalidPasswordException;
import com.oanda.fxtrade.api.InvalidUserException;
import com.oanda.fxtrade.api.MultiFactorAuthenticationException;
import com.oanda.fxtrade.api.SessionException;
import com.oanda.fxtrade.api.Transaction;

/**
 * LimitMapper is an fxTrade API example which detects the execution of limit orders and
 * maps the resultant market orders back to the limit order that spawned it.
 * @author Chris MacGregor
 */

public class LimitMapper {
	public static void main(String[] args)
	{
		if (args.length != 2)
		{
			System.out.println("Usage: LimitMapper [username] [password]");
			System.exit(1);
		}
		// Connect to FXServer
		FXClient fxclient = API.createFXGame();
		fxclient.setWithKeepAliveThread(true);
		System.out.print("Logging in as \"" + args[0] + "\"...");
		try { fxclient.login(args[0], args[1], "LimitMapper Test"); }
		catch (SessionException e) { System.exit(1); }
		catch (InvalidUserException e) { System.exit(1); }
		catch (InvalidPasswordException e) { System.exit(1); }
		catch (MultiFactorAuthenticationException e) { System.exit(1); }

		//Register transaction feed event
		System.out.print("login complete. Registering listeners...");
		LMEvent evt = new LMEvent();
		try { ((Account)fxclient.getUser().getAccounts().elementAt(0)).getEventManager().add(evt); }
		catch (AccountException err) { fxclient.logout(); System.exit(1); }
		catch (SessionException err) { fxclient.logout(); System.exit(1); }

		System.out.println("done. Watching for triggered limit orders for two minutes...");
		//Sleep for two minutes, let it run
		try { Thread.sleep(120000); }
		catch (InterruptedException e) {} //Interrupted, just keep running

		System.out.println("Two minute elapsed.");
		//Print the mapping
		evt.printReport();

		System.out.println("Logging out and exiting.");
		//Done, quit now
		fxclient.logout();
	}
}

class LMEvent extends FXAccountEvent
{
	private Vector<Transaction> unmappedTransactions;
	private Vector<TransactionPair> mappedTransactionPairs;

	public LMEvent()
	{
		super();
		unmappedTransactions = new Vector<Transaction>();
		mappedTransactionPairs = new Vector<TransactionPair>();
	}

	public boolean match(FXEventInfo EI)
	{
		//Match all FXEventInfos whose transaction's completion code indicates it to be an
		//order fulfillment
		FXAccountEventInfo AEI = (FXAccountEventInfo)EI;
		return AEI.getTransaction().getCompletionCode() == Transaction.FX_XFR_ORDER;
	}

	public void handle(FXEventInfo EI, FXEventManager EM)
	{
		FXAccountEventInfo AEI = (FXAccountEventInfo)EI;
		if (AEI.getTransaction().getType().equals("BuyMarket") ||
				AEI.getTransaction().getType().equals("SellMarket"))
		{
			//This transaction isn't a close order, so it is the result of closing a market order.
			//Put it in the temp. vector to be mapped later
			unmappedTransactions.addElement(AEI.getTransaction());
		}
		else
		{
			//This transactino is a close order, so its link number says which limit order it is
			//closing.  Map all transaction numbers on the stack to that link number.
			System.out.println("Detected newly executed limit order, upating map");
			for (int count = 0; count < unmappedTransactions.size(); count++)
			{
				mappedTransactionPairs.addElement(
						new TransactionPair(
								AEI.getTransaction().getTransactionLink(),
								((Transaction)unmappedTransactions.elementAt(count)).getTransactionNumber()));
			}
			unmappedTransactions.removeAllElements();
		}
	}

	void sort(Vector<TransactionPair> vec) {
		// for 1.1 compatibility, we don't use 1.2's sorting routines
		// slow sort, but good enough here.
		int sz = vec.size();
		for(int i = 0; i<sz; i++) {
			TransactionPair tpi = (TransactionPair) vec.elementAt(i);
			for(int j = i+1; j< sz; j++) {
				TransactionPair tpj = (TransactionPair) vec.elementAt(j);
				if(tpi.compareTo(tpj) == 1) {
					vec.setElementAt(tpj,i);
					vec.setElementAt(tpi,j);
					tpi = tpj;
				}
			}
		}
	}

	public void printReport() // Prints out all the maps that have been generated
	{
		//Collections.sort(mappedTransactionPairs); // only in 1.2 and on
		sort(mappedTransactionPairs);

		for (int count = 0; count < mappedTransactionPairs.size(); count++)
		{
			System.out.println((TransactionPair)mappedTransactionPairs.elementAt(count));
		}
	}
}

/**
 * Simple data blob class to keep track of two mapped transaction numbers
 */
class TransactionPair // implements Comparable
{
	private int limitTrans, orderTrans;

	public TransactionPair(int lt, int ot)
	{
		limitTrans = lt;
		orderTrans = ot;
	}

	public String toString()
	{
		return "Limit order #" + limitTrans + " spawned market order #" + orderTrans;
	}

	public int compareTo(Object o)
	{
		if (!(o instanceof TransactionPair))
			throw new ClassCastException("Cannot compare TransactionPair to " + o.getClass().getName());
		TransactionPair tp = (TransactionPair)o;

		//Sort first by the limit order...
		if (limitTrans < tp.limitTrans) return -1;
		else if (limitTrans > tp.limitTrans) return 1;
		else
		{
			//and second by the market order it spawned
			if (orderTrans < tp.orderTrans) return -1;
			else if (orderTrans > tp.orderTrans) return -1;
			else return 0;
		}
	}
}
