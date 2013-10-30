package com.oanda.fxtrade.api.test;

import com.oanda.fxtrade.api.API;
import com.oanda.fxtrade.api.FXClient;
import com.oanda.fxtrade.api.FXEventInfo;
import com.oanda.fxtrade.api.FXEventManager;
import com.oanda.fxtrade.api.FXRateEvent;
import com.oanda.fxtrade.api.FXRateEventInfo;
import com.oanda.fxtrade.api.InvalidPasswordException;
import com.oanda.fxtrade.api.InvalidUserException;
import com.oanda.fxtrade.api.MultiFactorAuthenticationException;
import com.oanda.fxtrade.api.SessionException;

/**
 * A simple event model example which prints all incoming rate ticks, and
 * watches a specific pair for tick changes
 */
public class RateTicker {
	public static void main(String[] args)
	{
		long sleepTime = 300000; //5 mins
		FXClient fxclient = null;

		if (args.length < 2) {
			System.out.println("Usage: RateTicker [username] [password]");
			System.exit(1);
		}

		if(args.length > 2) {
			sleepTime = Long.parseLong(args[2]);
		}

		if(args.length > 3 && args[3].equals("FXTrade")) {
			fxclient = API.createFXTrade();
		}
		else {
			fxclient = API.createFXGame();
		}

		// Connect to FXServer
		System.out.print("Logging in as \"" + args[0] + "\"...");
		fxclient.setWithRateThread(true);
		try { fxclient.login(args[0], args[1], "RateTicker Test"); }
		catch (SessionException e) { System.exit(1); }
		catch (InvalidUserException e) { System.exit(1); }
		catch (InvalidPasswordException e) { System.exit(1); }
		catch (MultiFactorAuthenticationException e) { System.exit(1); }

		//Register rate ticker event
		System.out.print("login complete. Registering listeners...");
		Ticker t = new Ticker();
		try { fxclient.getRateTable().getEventManager().add(t); }
		catch (SessionException e) { fxclient.logout(); System.exit(1); }

		//Register pair watcher event
		PairWatch pw = new PairWatch("EUR/USD");
		try { fxclient.getRateTable().getEventManager().add(pw); }
		catch (SessionException e) { fxclient.logout(); System.exit(1); }

		//Sleep for a minute, let it run
		System.out.println("done. Printing rate data...");
		try { Thread.sleep(sleepTime); }
		catch (InterruptedException e) {} //Interrupted, just keep running

		System.out.println("One minute elapsed.  Logging out and exiting.");
		//Done, quit now
		fxclient.logout();
	}
}

class Ticker extends FXRateEvent
{
	// No key set and no match implemented; this event matches all RateEventInfos

	public void handle(FXEventInfo EI, FXEventManager EM)
	{
		//Just print the tick
		FXRateEventInfo REI = (FXRateEventInfo) EI;
		System.out.println(REI.getPair() + ":" + REI.getTick());
	}
}

class PairWatch extends FXRateEvent
{
	public PairWatch(String s) { super(s); } //Watch for rates for the given pair

	public void handle(FXEventInfo EI, FXEventManager EM)
	{
		FXRateEventInfo REI = (FXRateEventInfo) EI;
		System.out.println(REI.getPair() + ":" + REI.getTick());
//		com.oanda.fxtrade.api.FXTick currTick = REI.getTick();
//		if (lastTick == null) // Init the tick value if no previous one is available
//		{
//			lastTick = REI.getTick();
//			return;
//		}
//		else //Compare the current and previous tick values
//		{
//			if (currTick.getBid() > lastTick.getBid())
//				System.out.println(REI.getPair() + " has gone up");
//			else if (currTick.getBid() < lastTick.getBid())
//				System.out.println(REI.getPair() + " has gone down");
//			else
//				System.out.println(REI.getPair() + " hasn't changed");
//			lastTick = currTick;
//		}
	}
}
