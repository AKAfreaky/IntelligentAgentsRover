package rover;

import java.util.Random;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.*;

import org.iids.aos.log.Log;
import org.iids.aos.systemservices.communicator.structs.AgentHandle;
import org.iids.aos.messagecenter.Envelope;
import org.iids.aos.exception.AgentScapeException;



public class MessagingRover extends Rover
{
	/*= Agent Attributes =*/
	protected int MAX_SPEED			= 4;
	protected int MAX_SCAN 			= 4;
	protected int CAPACITY 			= 1;
	public static final String TEAM_NAME	= "aa425";
	
	/*= Constants that depend on the agent attributes =*/
	protected double EFFICIENCY; // we can use less power if we don't go at full whack
	protected double SPEED;
	protected double SCAN_RANGE;
	// Step, as in move distance, to give the searching a good coverage. Based on a hexagonal grid.
	protected double X_STEP;
	protected double Y_STEP;
	
	/*= Constants for the different modes =*/
	public static final int SEARCH_MODE 	= 0;
	public static final int COLLECT_MODE 	= 1; //I find retrieval hard to type consistently
	public static final int DEPOSIT_MODE 	= 2;
	public static final int RE_SEARCH_MODE 	= 3;
	public static final int WAIT_MODE		= 4;
	
	/*= Member Variables =*/
	private WorldMap theMap;
	private final ImmutableVector basePos; // More useful as an easy 0,0 vector
	private Vector lastSearchPos; // So we can return to it after eating resources
	private Vector nextMove;
	private Vector activeResource; // The resource we are currently trying to eat
	
	private int xSteps, xStepCount, ySteps, yStepCount;	 // For the searching.
	
	private int MODE; // The current aim
	
	private Message HELLO_MSG 		= new Message( MsgType.HELLO,		"S",		0);
	private Message HELLO_NEG 		= new Message( MsgType.HELLO,		"N",		0);
	private Message RES_FOUND 		= new Message( MsgType.RESOURCE,	"A:%d:%d" , 0);
	private Message RES_GONE  		= new Message( MsgType.RESOURCE,	"R:%d:%d" , 0);
	private Message RES_ACTIVE		= new Message( MsgType.RESOURCE,	"C:%d:%d" , 0);
	private Message ORD_START 		= new Message( MsgType.ORDER,		"S:%d:%d" , 0);
	private Message ORD_PRIO  		= new Message( MsgType.ORDER,		"P:%d" ,	0);
	private Message ORD_PRIO_UPDATE	= new Message( MsgType.ORDER,		"U" ,		0);
	
	
	private ArrayList<Integer> scouts;
	private ArrayList<Integer> lifters;
	
	private int startStep;
	private volatile boolean canStart;
	private volatile int canNegotiate;
	private int myHandleHash;
	
	protected int roverType = 0;
	
	private volatile int resPriority;
	
	public MessagingRover()
	{
		Log.console("MessagingRover start");
		setTeam(TEAM_NAME);
		
		EFFICIENCY				= 1.0f; // we can use less power if we don't go at full whack
		SPEED					= MAX_SPEED * EFFICIENCY;
		SCAN_RANGE				= MAX_SCAN	* EFFICIENCY;
		X_STEP					= Math.sqrt(3) * 2 * SCAN_RANGE;
		Y_STEP					= 3.0f * SCAN_RANGE;
			
		// Assume spawn is 0,0. Map is toroid, so should be no problems.
		basePos			= new ImmutableVector(0.0f, 0.0f);
		lastSearchPos 	= new Vector();
		nextMove		= new Vector();
		activeResource 	= new Vector();
		theMap			= new WorldMap();
		
		xSteps 		= 0;
		xStepCount 	= 0;
		ySteps		= 0;
		yStepCount 	= 0;
		
		scouts		= new ArrayList<Integer>();
		lifters		= new ArrayList<Integer>();
				
		startStep 		= 0;
		canStart		= false;
		canNegotiate	= 0;
		myHandleHash	= 0;
		
		resPriority		= 0;
		
		MODE = SEARCH_MODE;
	}
	
	
	// Recalculate these vars as child classes will have different attributes
	public void init()
	{
		EFFICIENCY				= 1.0f; // we can use less power if we don't go at full whack
		SPEED					= MAX_SPEED * EFFICIENCY;
		SCAN_RANGE				= MAX_SCAN	* EFFICIENCY;
		X_STEP					= Math.sqrt(3) * 2 * SCAN_RANGE;
		Y_STEP					= 3.0f * SCAN_RANGE;
		
		if ( roverType == 2 )
			MODE = WAIT_MODE;
		else
			MODE = SEARCH_MODE;
		
		Log.console(myHandleHash + ": Rover Type: " + roverType + ", X_STEP: " + X_STEP + ", Y_STEP: " + Y_STEP);
	}
	
	
	//Register for messages
	void register()
	{
		try 
		{
			synchronized(list(TEAM_NAME).keySet())
			{
	            register (getPrimaryHandle(),TEAM_NAME);
	            Log.console("register success!");
	        }
        } 
		catch (AgentScapeException e)
		{
            Log.console("register failed", e);
            return;
        }
	}
	
	
	//Send a message
	void send(Message msg)
	{
		try 
		{
			Envelope out = new Envelope();
			//identify my handle
			AgentHandle myHandle = getPrimaryHandle();
			//set data
			out.setData(msg);
			//set handle
			out.setFromHandle(myHandle);
			
			//add each agent's handle in time
			for(AgentHandle ag : list(TEAM_NAME).keySet())
			{
				//send if not myself
				if(! ag.equals(myHandle)){
					//create an envelop for each
					out.addTarget(ag);
				}
			}
			
			if (out.getTargetCount() > 0)
			{
				sendMessage(out);
				//sleep for a bit
				Thread.sleep(100);
			}
			else
			{
				Log.console("No available targets!");
			}
			
		}
		catch (AgentScapeException e)
		{
	           Log.console("Problem sending message, caused by " +
	                    e.getCause().getClass().getName() + ", reason: "
	                    + e.getMessage());
	    }
		catch (InterruptedException e) 
		{
			e.printStackTrace();
	    }
	}


	//listen message function
	void listenForMessages() 
	{
		//keep listening while agent is running
		while(agentRunning()) 
		{
			try 
			{
				//non-blocking call to receive.
				List<Envelope> envs = receiveMessages(false);
				for(Envelope env : envs) 
				{
					Message msg = (Message) env.getData();
					handleMessage(msg, env.getFromHandle());
				}
			} 
			catch (AgentScapeException e) 
			{
				e.printStackTrace();
				break;
			}
			
			try 
			{
				//sleep for a bit
				Thread.sleep(100);
			} 
			catch (InterruptedException e) 
			{
				e.printStackTrace();
				break;
			}
			
		}		
	}

	//run an agent
	public void run()
	{
		super.run();
		Log.console("Run called!");
		register();
		listenForMessages();
	}
	
	
	@Override
	void begin() 
	{
		//called when the world is started
		Log.console("BEGIN!");
		
		if (roverType == 2)
		{
			HELLO_MSG.setData("L");
		}
		
		if (roverType != 3)
			send(HELLO_MSG);
		
		try 
		{
			// getWorld...() is invalid before the game starts and we can't
			// create an object here because agentscape is buggy.
			theMap.init( getWorldWidth(), getWorldHeight() );
			
			myHandleHash = getPrimaryHandle().hashCode();
			
			if (roverType == 1)
				scouts.add(myHandleHash);
			else if(roverType == 2)
				lifters.add(myHandleHash);
						
			if (roverType != 3)
			{
				initialNegotiation();
			}
			else
			{
				xSteps = (int)Math.floor(getWorldWidth() / X_STEP);
				ySteps = (int)Math.floor(getWorldHeight() / Y_STEP);
				scan(SCAN_RANGE);
			}
			
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		
	}
	
	public void initialNegotiation() throws Exception
	{
		//sleep until we've heard from everybody
		while (list(TEAM_NAME).keySet().size() > (scouts.size() + lifters.size()))
		{
			Thread.sleep(100);
		}
		
		canNegotiate++;
		send(HELLO_NEG);
		
		//sleep until everybody has heard from everybody
		while (list(TEAM_NAME).keySet().size() > canNegotiate)
		{
			Thread.sleep(100);
		}
		
		Log.console("My hash: " + myHandleHash);
		
		boolean boss = true;
		for(AgentHandle ag : list(TEAM_NAME).keySet())
		{
			Log.console("Other hash: " + ag.hashCode());
			if( myHandleHash > ag.hashCode() )
			{
				boss = false;
				break;
			}
		}
		
		xSteps = (int)Math.floor(getWorldWidth() / X_STEP);
		
		if( boss )
		{
			Log.console("We're the boss");
			sendOrders();
			canStart = true;
			
			if (roverType == 1)
			{
				// Scan first in case there's food nearby
				scan(SCAN_RANGE);
			}
		}
		else
		{
			Log.console("Not the boss");
			while(!canStart)
			{
				Thread.sleep(100);
			}
			
			if (roverType == 1)
			{
				nextMove.setX( (startStep % 2) == 0 ? 0.0f : X_STEP/2 );
				nextMove.setY( startStep * Y_STEP );
				
				move(nextMove.getX(), nextMove.getY(), SPEED);
			}
		}
		
		if (roverType == 2)
		{
			waitForResources();
		}
	}
	
	
	public void sendOrders()
	{
		// An approx. number of steps to cover the whole map
		ySteps = (int)Math.floor(getWorldHeight() / 12);//Y_STEP); // Really shouldn't hard code this, but I'm not going to change the scout values and cba messaging the Y_STEP
				
		int yChunk 		= (ySteps / scouts.size());
		int extra  		= (ySteps % scouts.size() == 0) ? 0 : 1; 
		yChunk 		   += extra;
		int startSteps 	= (roverType == 1) ? yChunk : 0;
		
		for( Integer scout : scouts )
		{
			if (scout != myHandleHash )
			{
				String data = String.format("S:%d:%d", startSteps, yChunk);
				ORD_START.setData(data);
				ORD_START.setTarget(scout.intValue());
				send(ORD_START);
				
				startSteps += yChunk;
			}
		}
		
		startStep = 0;
		ySteps = yChunk + extra;
		
		int priority = (roverType == 2) ? 1 : 0;
		
		for( Integer lifter : lifters )
		{
			if (lifter != myHandleHash)
			{
				String data = String.format("P:%d", priority);
				ORD_PRIO.setData(data);
				ORD_PRIO.setTarget(lifter.intValue());
				
				send(ORD_PRIO);
				
				priority++;
			}
		}
		
	}
	
	public void waitForResources()
	{
		// Wait until we're the next lifter to go
		while(resPriority > 0)
		{
			try
			{
				Thread.sleep(1000);
			} 
			catch (Exception e) 
			{
				e.printStackTrace();
			}
		}
		
		// Wait until there's a resource to eat
		ImmutableVector resource;
		while((resource = theMap.closestResource()) == null)
		{
			try
			{
				Thread.sleep(1000);
			} 
			catch (Exception e) 
			{
				e.printStackTrace();
			}
		}
		
		// Get the movement vector the the resources
		ImmutableVector theMove = theMap.offsetToPosition( resource.getX(), resource.getY() );
		
		// Update our log
		activeResource.setX( resource.getX() );
		activeResource.setY( resource.getY() );
		theMap.activateResource( resource );
		
		// Update other players
		long xPos = resource.getXLongBits();
		long yPos = resource.getYLongBits();
		
		String data = String.format("C:%d:%d", xPos, yPos);
		RES_ACTIVE.setData(data);
		send(RES_ACTIVE);
		
		// Do the move
		try
		{
			move( theMove.getX(), theMove.getY(), SPEED );
			nextMove.setX( theMove.getX() );
			nextMove.setY( theMove.getY() );
			MODE = COLLECT_MODE;
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		
		// Tell other players to change their priority and reduce ours
		send(ORD_PRIO_UPDATE);
		resPriority = 99;
		
	}

	
	@Override
	void end() 
	{
		// called when the world is stopped
		// the agent is killed after this
		Log.console("END!");
	}
	
	public void handleHello(Message msg, AgentHandle source)
	{
		if (msg.getData().equals("S"))
		{
			scouts.add(source.hashCode());
		}
		else if (msg.getData().equals("L"))
		{
			lifters.add(source.hashCode());
		}
		else if (msg.getData().equals("N"))
		{
			canNegotiate++;
		}
		else
		{
			Log.console("Unknown hello.");
		}
	}
	
	
	public void handleResourceMsg(Message msg, AgentHandle source)
	{
		Pattern p = Pattern.compile("(\\w):([0-9]+):([0-9]+)");
		Matcher m = p.matcher( msg.getData() );
		if( m.matches())
		{
			long xPosL	= Long.valueOf( m.group( 2 ) );
			long yPosL	= Long.valueOf( m.group( 3 ) );
			double xPos = Double.longBitsToDouble( xPosL );
			double yPos = Double.longBitsToDouble( yPosL );
			
			if (m.group( 1 ).equals("A"))
			{
				theMap.addResourceAbs( xPos, yPos );
			}
			else if (m.group( 1 ).equals("R"))
			{
				theMap.removeResource( new ImmutableVector( xPos, yPos ) );
			}
			else if (m.group( 1 ).equals("C"))
			{
				theMap.activateResource( new ImmutableVector( xPos, yPos ) );
			}
			else 
			{
				Log.console("Unrecognised resource message (" + msg.getData() + ")");
			}
		}
		else
		{
			Log.console("Resource message invalid! (" + msg.getData() +")");
		}
	}
	
	
	public void handleOrder(Message msg, AgentHandle source)
	{
		if(msg.getTarget() == myHandleHash || msg.getTarget() == 0)
		{
			Pattern p = Pattern.compile("(\\w).*");
			Matcher m = p.matcher( msg.getData() );
			if( m.matches() )
			{
				if( m.group( 1 ).equals("S"))
				{
					Pattern p2 = Pattern.compile("\\w:([0-9]+):([0-9]+)");
					Matcher m2 = p2.matcher( msg.getData() );
					if( m2.matches())
					{
						startStep	= Integer.parseInt( m2.group( 1 ) );
						ySteps		= Integer.parseInt( m2.group( 2 ) );							
					}
					else
					{
						Log.console("Search start order invalid! (" + msg.getData() +")");
					}
					
					canStart = true;
				}
				else if( m.group( 1 ).equals("P"))
				{
					Pattern p2 = Pattern.compile("\\w:([0-9]+)");
					Matcher m2 = p2.matcher( msg.getData() );
					if( m2.matches())
					{
						resPriority	= Integer.parseInt( m2.group( 1 ) );
					}
					else
					{
						Log.console("Priority set order invalid! (" + msg.getData() +")");
					}
					
					canStart = true;
				}
				else if( m.group( 1 ).equals("U"))
				{
					resPriority--;
				}
			
			
			}
		}
	}
	
	
	public void handleMessage(Message msg, AgentHandle source)
	{
		// Java didn't like my enum in a switch statement :(
		if(msg.getType() == MsgType.HELLO)
		{
			handleHello(msg, source);
		}
		else if(msg.getType() == MsgType.RESOURCE)		
		{
			handleResourceMsg(msg, source);
		}
		else if(msg.getType() == MsgType.ORDER)
		{
			handleOrder(msg, source);
		}
		else
		{
			Log.console("Received unrecognised/empty message");
		}
	}
	
	
	public void handleCollectFailure()
	{
		synchronized(theMap)
		{
			theMap.removeResource(activeResource);
			resPriority = Math.max(lifters.size() - theMap.numActiveResources() - 1, 0); //-1 as prio 0-based.
		}
				
		activeResource.setX(0.0f);
		activeResource.setY(0.0f);
		
		if ( getCurrentLoad() > 0 )
		{
			MODE = DEPOSIT_MODE;
		}
		else if (theMap.numResourceFound() > 0)
		{
			MODE = COLLECT_MODE;
		}
		else
		{
			if (roverType == 2)
			{
				MODE = WAIT_MODE;
			}
			else
			{
				MODE = RE_SEARCH_MODE;
			}
		}
	}
	
	
	/*= Stuff to do after we finish a move =*/
	public boolean handleMove(PollResult pr)
	{
		boolean retVal = false;
		
		theMap.updatePosistion( nextMove.getX(), nextMove.getY() );
		
		try 
		{
			if (MODE == SEARCH_MODE)
			{
				if (roverType != 2)
					scan(SCAN_RANGE);
				else
					Log.console(myHandleHash + ":Lifters can't scan!");
			}
			else if (MODE == COLLECT_MODE)
			{
				collect();
			}
			else if (MODE == DEPOSIT_MODE)
			{
				deposit();
			}
			else if (MODE == WAIT_MODE)
			{
				waitForResources();
			}
		} 
		catch (Exception e)
		{
			String message = e.getMessage();
			
			// Handle the resource running dry
			if (message != null && message.equals("No resources to collect"))
			{
				handleCollectFailure();
				retVal = true;
			}
		
			e.printStackTrace();
		}
		
		return retVal;
	}
	
	
	/*= Stuff to do after we finish a scan =*/
	public boolean handleScan(PollResult pr)
	{
		for(ScanItem item : pr.getScanItems()) 
		{
			if(item.getItemType() == ScanItem.RESOURCE) 
			{
				Log.console("Resource found at: " + item.getxOffset() + ", " + item.getyOffset());
								
				theMap.addResource( item.getxOffset(), item.getyOffset() );
								
				long xPos = Double.doubleToRawLongBits( 
								theMap.normaliseToValue( 
									theMap.getCurrPosition().getX() + item.getxOffset(), getWorldWidth()));
				long yPos = Double.doubleToRawLongBits( 
								theMap.normaliseToValue( 
									theMap.getCurrPosition().getY() + item.getyOffset(), getWorldHeight()));
				
				if (roverType != 3)
				{
					String data = String.format("A:%d:%d", xPos, yPos);
					RES_FOUND.setData(data);
					send(RES_FOUND);
				}
				else
				{
					MODE = COLLECT_MODE;
					lastSearchPos.setX( theMap.getCurrPosition().getX() );
					lastSearchPos.setY( theMap.getCurrPosition().getY() );
				}
			} 
			else if(item.getItemType() == ScanItem.BASE) 
			{
				Log.console("Base found at: " + item.getxOffset() + ", " + item.getyOffset());
				// Not completely sure why I bother, I don't think we can raid them.
				theMap.addBase( item.getxOffset(), item.getyOffset() );
			} 
			else 
			{
				Log.console("Rover found at: " + item.getxOffset() + ", " + item.getyOffset());
			}
		}
		
		return true;
	}
	
	
	/*= Stuff to do after we finish collecting =*/
	public boolean handleCollect(PollResult pr)
	{
		boolean willMove = true;
		switch( pr.getResultStatus() )
		{
			case PollResult.COMPLETE:
				
				if ( getCurrentLoad() < CAPACITY )
				{
					try
					{
						collect();
						willMove = false;
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}
				}
				else
				{
					MODE = DEPOSIT_MODE;
				}
				break;
			default:
				handleCollectFailure();
			break;
		}
		return willMove;
	}
	
	
	/*= Stuff to do after we finish depositing =*/
	public boolean handleDeposit(PollResult pr)
	{
		boolean willMove = false;
		if (getCurrentLoad() > 0)
		{
			try
			{
				deposit();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		else
		{
			// If we know of more resources, keep gathering them.
			if ( (theMap.numResourceFound() > 0) || !activeResource.equals(basePos) )
			{
				MODE = COLLECT_MODE;
			}
			else
			{
				MODE = (roverType == 2) ? WAIT_MODE : RE_SEARCH_MODE;
			}
			willMove = true;
		}
		
		return willMove;
	}
	
	
	// This is called when one of the actions has completed
	@Override
	void poll(PollResult pr) 
	{
		if(pr.getResultStatus() == PollResult.FAILED) 
		{
			Log.console("Ran out of power...");
			return;
		}
		
		boolean willMove = false;
		
		switch(pr.getResultType()) 
		{
			case PollResult.MOVE:
				willMove = handleMove(pr);				
				break;
			case PollResult.SCAN:
				willMove = handleScan(pr);
				break;
			case PollResult.COLLECT:
				willMove = handleCollect(pr);
				break;
			case PollResult.DEPOSIT:
				willMove = handleDeposit(pr);
				break;
		}
		
		// now move again
		if (willMove)
		{
			Random rand = new Random();
			try 
			{
				if (MODE == COLLECT_MODE)
				{
					ImmutableVector resource;
					if( activeResource.equals(basePos) ) // basePos should always be 0.0f 0.0f
					{
						resource = theMap.closestResource();
						if (resource != null)
						{
							activeResource.setX( resource.getX() );
							activeResource.setY( resource.getY() );
						}
						else // There are no more resources to collect, so don't move anywhere and go back to searching 
						{
							if (roverType == 2)
							{
								resource = basePos;
								MODE = WAIT_MODE;
							}
							else
							{
								resource = theMap.getCurrPosition();
								MODE = RE_SEARCH_MODE;
							}
						}
					}
					else
					{
						resource = activeResource;
					}
					
					ImmutableVector move = theMap.offsetToPosition(resource.getX(), resource.getY());
					
					nextMove.setX( move.getX() );
					nextMove.setY( move.getY() );
				}
				else if (MODE == DEPOSIT_MODE || MODE == WAIT_MODE)
				{
					ImmutableVector move = theMap.offsetToHome();
					
					nextMove.setX( move.getX() );
					nextMove.setY( move.getY() );
				}
				else if (MODE == RE_SEARCH_MODE)
				{
					MODE = SEARCH_MODE;
					nextMove.setX( lastSearchPos.getX() );
					nextMove.setY( lastSearchPos.getY() );
					
				}
				else
				{					
					// We search row by row, with a hexagonal coverage.
					if (xStepCount < xSteps)
					{
						nextMove.setX( X_STEP );
						nextMove.setY( 0.0f );
						xStepCount++;
					}
					else if (yStepCount < ySteps)
					{
						nextMove.setX( (yStepCount % 2 == 0) ? (X_STEP / 2) : (-X_STEP / 2) );
						nextMove.setY( Y_STEP );
						xStepCount = 0;
						yStepCount++;
					}
					else // Restart the search
					{
						ImmutableVector home = theMap.offsetToHome();
						nextMove.setX( home.getX() );
						nextMove.setY( home.getY() );
						xStepCount = 0;
						yStepCount = 0;
					}
				}
				
				move(nextMove.getX(), nextMove.getY(), SPEED);
			} 
			catch (Exception e) 
			{
				e.printStackTrace();
			}
		}
	}

}
