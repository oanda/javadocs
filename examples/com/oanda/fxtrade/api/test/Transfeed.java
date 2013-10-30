package com.oanda.fxtrade.api.test;
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

public class Transfeed {
	public static void main(String[] args)
	{
		if (args.length != 2)
		{
			System.out.println("Usage: Transfeed [username] [password]");
			System.exit(1);
		}

		// Connect to FXServer
		FXClient fxclient = API.createFXGame();
		fxclient.setWithKeepAliveThread(true);
		System.out.print("Logging in as \"" + args[0] + "\"...");
		try { fxclient.login(args[0], args[1], "TransFeed Test"); }
		catch (SessionException e) { System.exit(1); }
		catch (InvalidUserException e) { System.exit(1); }
		catch (InvalidPasswordException e) { System.exit(1); }
		catch (MultiFactorAuthenticationException e) { System.exit(1); }

		//Register transaction feed event
		System.out.print("login complete. Registering listener...");
		TFEvent evt = new TFEvent();
		try { ((Account)fxclient.getUser().getAccounts().elementAt(0)).getEventManager().add(evt); }
		catch (AccountException err) { fxclient.logout(); System.exit(1); }
		catch (SessionException err) { fxclient.logout(); System.exit(1); }

		//Sleep for a minute, let it run
		System.out.println("done. Listening for transactions for one day...");
		try { Thread.sleep(60000 * 60 * 24); }
		catch (InterruptedException e) {} //Interrupted, just keep running

		//Done, quit now
		System.out.println("One minute elapsed.  Logging out and exiting.");
		fxclient.logout();
	}
}

class TFEvent extends FXAccountEvent
{
	//No match() method override and no key set; this event matches all eventinfos

	public void handle(FXEventInfo EI, FXEventManager EM)
	{
		//Just print out the transaction that generated this event
		FXAccountEventInfo AEI = (FXAccountEventInfo) EI;
		long ltime = System.currentTimeMillis() / 1000;
		System.out.println(" At ts = " + ltime + " detected a trans: ");
		System.out.println(AEI.getTransaction());
	}
}
