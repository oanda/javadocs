package com.oanda.fxtrade.api.test;
import com.oanda.fxtrade.api.API;
import com.oanda.fxtrade.api.Account;
import com.oanda.fxtrade.api.FXClient;
import com.oanda.fxtrade.api.FXEventInfo;
import com.oanda.fxtrade.api.FXEventManager;
import com.oanda.fxtrade.api.FXPair;
import com.oanda.fxtrade.api.FXRateEvent;
import com.oanda.fxtrade.api.FXRateEventInfo;
import com.oanda.fxtrade.api.InvalidPasswordException;
import com.oanda.fxtrade.api.InvalidUserException;
import com.oanda.fxtrade.api.MarketOrder;
import com.oanda.fxtrade.api.MultiFactorAuthenticationException;
import com.oanda.fxtrade.api.OAException;
import com.oanda.fxtrade.api.RateTable;
import com.oanda.fxtrade.api.RateTableException;
import com.oanda.fxtrade.api.SessionDisconnectedException;
import com.oanda.fxtrade.api.SessionException;

public class TrailingSL
{
	static Object notifyer = new Object();

	public static void main(String[] args)
	{
		if (args.length != 2)
		{
			System.out.println("Usage: TrailingSL [username] [password]");
			System.exit(1);
		}
		// Connect to FXServer
		FXClient fxclient = API.createFXGame();
		fxclient.setWithRateThread(true);
		System.out.print("Logging in as \"" + args[0] + "\"...");
		try { fxclient.login(args[0], args[1], "TrailingSL Test"); }
		catch (SessionException e) { System.exit(1); }
		catch (InvalidUserException e) { System.exit(1); }
		catch (InvalidPasswordException e) { System.exit(1); }
		catch (MultiFactorAuthenticationException e) { System.exit(1); }

		RateTable ratetable = null;
		try { ratetable = fxclient.getRateTable(); }
		catch (SessionDisconnectedException e) { fxclient.logout(); System.exit(1); }

		Account account = null;
		try { account = (Account)fxclient.getUser().getAccounts().elementAt(0); }
		catch (SessionException e) { fxclient.logout(); System.exit(1); }

		//Creating order to watch
		System.out.print("login complete. Creating new marked order...");
		FXPair pair = API.createFXPair("EUR/USD");
		com.oanda.fxtrade.api.FXTick tick = null;
		try { tick = ratetable.getRate(pair); }
		catch (RateTableException e) { fxclient.logout(); System.exit(1); }

		double delta = (tick.getAsk() - tick.getBid()) * 2.2;

		MarketOrder order = API.createMarketOrder();
		order.setPair(pair);
		order.setUnits(5);
		order.setStopLoss(API.createStopLossOrder(tick.getAsk() - delta));

		try { account.execute(order); }
		catch (OAException e) { fxclient.logout(); System.exit(1); }

		//Creating event to watch it
		System.out.print("created. Registering listener...");
		TSLEvent event = new TSLEvent(pair, account, order, delta);
		ratetable.getEventManager().add(event);

		//Wait for the order to close
		System.out.println("done.  Monitoring stop loss...");
		try { synchronized(notifyer) { notifyer.wait(); } }
		catch (InterruptedException e) {} //Interrupted, just keep running

		//Done, quit now
		System.out.println("Logging out and exiting.");
		fxclient.logout();
	}
}

class TSLEvent extends FXRateEvent
{
	private Account account;
	private MarketOrder watchedOrder;
	private double delta;
	private boolean watchingBuyOrder;
	private double currentSL;

	public TSLEvent(FXPair _pair, Account _account, MarketOrder _watchedOrder, double _delta)
	{
		super(_pair.toString());
		account = _account;
		watchedOrder = _watchedOrder;
		delta = _delta;
		watchingBuyOrder = (watchedOrder.getUnits() > 0);
		currentSL = watchedOrder.getStopLoss().getPrice();
		setTransient(false);
	}

	// No match override so match all ticks of the given pair

	public void handle (FXEventInfo EI, FXEventManager EM)
	{
		FXRateEventInfo REI = (FXRateEventInfo)EI;
		try
		{
			//Check if the market order has already closed, if so, deregister event and end program
			com.oanda.fxtrade.api.MarketOrder oldOrder = account.getTradeWithId(watchedOrder.getTransactionNumber());
			if (oldOrder == null)
			{
				System.out.println("Watched order has closed, deregistering event");
				EM.remove(this);
				synchronized(TrailingSL.notifyer)
				{
					TrailingSL.notifyer.notify();
				}
			}

			//Sometimes ticks with identical rates come in, filter these out
			double error = REI.getTick().getAsk() / 200000;

			boolean handleCondition = ( watchingBuyOrder
					? REI.getTick().getAsk() - delta - error > currentSL
					: REI.getTick().getBid() - delta - error < currentSL );

			if (!handleCondition)
			{
				System.out.println("New tick doesn't warrant changing stop loss");
				return;
			}

			//Update the stop loss on the order
			System.out.print("Updating stop loss: " + currentSL + "->");
			currentSL = watchingBuyOrder
					? REI.getTick().getAsk() - delta
					: REI.getTick().getBid() + delta;
			oldOrder.getStopLoss().setPrice(currentSL);
			System.out.println(currentSL);
			account.modify(oldOrder);
		}
		catch (OAException err)
		{
			System.err.println("Encountered error handling stop loss change, deregistering event");
			EM.remove(this);
			synchronized(TrailingSL.notifyer)
			{
				TrailingSL.notifyer.notify();
			}
		}
	}
}
