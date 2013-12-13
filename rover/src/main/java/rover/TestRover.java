package rover;

import java.util.Random;

import org.iids.aos.log.Log;


public class TestRover extends Rover
{
	/*= Agent Attributes =*/
	public static final int MAX_SPEED 		= 3;
	public static final int MAX_SCAN		= 3;
	public static final int CAPACITY		= 3;
	public static final String TEAM_NAME	= "aa425";
	
	/*= Constants that depend on the agent attributes =*/
	public static final double EFFICIENCY	= 1.0f; // we can use less power if we don't go at full whack
	public static final double SPEED		= MAX_SPEED * EFFICIENCY;
	public static final double SCAN_RANGE	= MAX_SCAN	* EFFICIENCY;
	// Step, as in move distance, to give the searching a good coverage. Based on a hexagonal grid.
	public static final double X_STEP		= Math.sqrt(3) * 2 * SCAN_RANGE;
	public static final double Y_STEP		= 3.0f * SCAN_RANGE;
	
	/*= Constants for the different modes =*/
	public static final int SEARCH_MODE = 0;
	public static final int COLLECT_MODE = 1; //I find retrieval hard to type consistently
	public static final int DEPOSIT_MODE = 2;
	public static final int RE_SEARCH_MODE = 4;
	
	/*= Member Variables =*/
	private WorldMap theMap;
	private final ImmutableVector basePos; // More useful as an easy 0,0 vector
	private Vector lastSearchPos; // So we can return to it after eating resources
	private Vector nextMove;
	private Vector activeResource; // The resource we are currently trying to eat
	
	private int xSteps, xStepCount, ySteps, yStepCount;	 // For the searching.
	
	private int MODE; // The current aim
	
	public TestRover() 
	{
		Log.console("TestRover start");
		setTeam(TEAM_NAME);
		
		// Assume spawn is 0,0. Map is toroid, so should be no problems.
		basePos			= new ImmutableVector(0.0f, 0.0f);
		lastSearchPos 	= new Vector();
		nextMove		= new Vector();
		activeResource 	= new Vector();
		theMap			= new WorldMap();
		
		xSteps 		= 0;
		xStepCount 	= 0;
		xSteps		= 0;
		yStepCount 	= 0;
		
		MODE = SEARCH_MODE;
		
		try 
		{
			// Set attributes for this rover
			// speed, scan range, max load have to add up to <= 9
			// Check because I can be stupid.
			if ( ( MAX_SPEED + MAX_SCAN + CAPACITY ) <= 9)
			{
				setAttributes( MAX_SPEED , MAX_SCAN , CAPACITY );
			}
			else
			{
				Log.console("Attributes invalid! Using defaults...");
				setAttributes(4, 4, 1);
			}
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		
	}

	@Override
	void begin() 
	{
		//called when the world is started
		Log.console("BEGIN!");
		
		try 
		{
			// getWorld...() is invalid before the game starts and we can't
			// create an object here because agentscape is buggy.
			theMap.init( getWorldWidth(), getWorldHeight() );
			
			// An approx. number of steps to cover the whole map
			xSteps = (int)Math.floor(getWorldWidth() / X_STEP);
			ySteps = (int)Math.floor(getWorldHeight() / Y_STEP);
			
			// Scan first in case there's food nearby
			scan(SCAN_RANGE);
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		
	}

	@Override
	void end() 
	{
		// called when the world is stopped
		// the agent is killed after this
		Log.console("END!");
	}
	
	
	/*= Stuff to do after we finish a move =*/
	public boolean handleMove(PollResult pr)
	{
		boolean retVal = false;
		
		Log.console("Move complete.");	
		
		theMap.updatePosistion( nextMove.getX(), nextMove.getY() );
		
		try 
		{
			if (MODE == SEARCH_MODE)
			{
				Log.console("Scanning...");
				scan(SCAN_RANGE);
			}
			else if (MODE == COLLECT_MODE)
			{
				Log.console("Collecting...");
				collect();
			}
			else if (MODE == DEPOSIT_MODE)
			{
				Log.console("Depositing...");
				deposit();
			}
		} 
		catch (Exception e)
		{
			String message = e.getMessage();
			
			// Handle the resource running dry
			if (message != null && message.equals("No resources to collect"))
			{
				Log.console("Collect failed. (No resources left)");
				
				theMap.removeResource(activeResource);
				
				activeResource.setX(0.0f);
				activeResource.setY(0.0f);
				if ( getCurrentLoad() > 0 )
				{
					Log.console("Returning to base.");
					MODE 	= DEPOSIT_MODE;
					retVal	= true;
				}
				else if (theMap.numResourceFound() > 0)
				{
					Log.console("Retrieving different resources");
					MODE 	= COLLECT_MODE;
					retVal 	= true;
				}
				else
				{
					Log.console("Returning to search.");
					MODE 	= RE_SEARCH_MODE;
					retVal 	= true;
				}
			}
		
			e.printStackTrace();
		}
		
		return retVal;
	}
	
	
	/*= Stuff to do after we finish a scan =*/
	public boolean handleScan(PollResult pr)
	{
		Log.console("Scan complete");
					
		for(ScanItem item : pr.getScanItems()) 
		{
			if(item.getItemType() == ScanItem.RESOURCE) 
			{
				Log.console("Resource found at: " + item.getxOffset() + ", " + item.getyOffset());
				
				lastSearchPos.setX(theMap.getCurrPosition().getX());
				lastSearchPos.setY(theMap.getCurrPosition().getY());
				
				theMap.addResource( item.getxOffset(), item.getyOffset() );
				
				MODE = COLLECT_MODE;
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
				Log.console("Collect complete.");
				
				if ( getCurrentLoad() < CAPACITY )
				{
					try
					{
						Log.console("Collecting again.");
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
					Log.console("Returning to base.");
					MODE = DEPOSIT_MODE;
				}
				break;
			default:
				// Kinda useless, the service throws an exception instead.
				Log.console("Collect failed.");
				if ( getCurrentLoad() > 0 )
				{
					Log.console("Returning to base.");
					MODE = DEPOSIT_MODE;
				}
				else
				{
					Log.console("Returning to search.");
					MODE = RE_SEARCH_MODE;
				}
			break;
		}
		return willMove;
	}
	
	
	/*= Stuff to do after we finish depositing =*/
	public boolean handleDeposit(PollResult pr)
	{
		boolean willMove = false;
		Log.console("Deposit complete.");
		if (getCurrentLoad() > 0)
		{
			try
			{
				Log.console("Depositing again...");
				deposit();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		else
		{
			Log.console("Deposited all resources.");
			// If we know of more resources, keep gathering them.
			MODE = (theMap.numResourceFound() > 0) ? COLLECT_MODE : RE_SEARCH_MODE;
			willMove = true;
		}
		
		return willMove;
	}
	

	@Override
	void poll(PollResult pr) 
	{
		// This is called when one of the actions has completed

		Log.console("Remaining Power: " + getEnergy());
		
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
							resource = theMap.getCurrPosition();
							MODE = RE_SEARCH_MODE;
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
				else if (MODE == DEPOSIT_MODE)
				{
					Log.console("Moving to base...");
					ImmutableVector move = theMap.offsetToHome();
					
					nextMove.setX( move.getX() );
					nextMove.setY( move.getY() );
				}
				else if (MODE == RE_SEARCH_MODE)
				{
					Log.console("Moving to last search position");
					MODE = SEARCH_MODE;
					nextMove.setX( lastSearchPos.getX() );
					nextMove.setY( lastSearchPos.getY() );
					
				}
				else
				{
					Log.console("Moving for search...");
					
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
				
				Log.console("Moving to (" + nextMove.getX() + ", " + nextMove.getY() + ")");
				move(nextMove.getX(), nextMove.getY(), SPEED);
			} 
			catch (Exception e) 
			{
				e.printStackTrace();
			}
		}
	}

}
