  Here is the current Version Update:
  
  

  Game is now relatively fully functioning (Minus Sound/Main Menu and extra features like different warps)
I'll design the sound aspect next and then the main menu, then I'll handle extra warps and such
For Anyone interested I would for sure read through the entire code, I've done my best to keep proper comments
Sections with not as many comments should not be tampered with, I've likely already tested them thoroughly.
If you make a change that affects the Game positively and you feel the need to tamper with one of those sections
Let me know and I'll break it down. The areas with the most comments are the most open! 


Still, I've left enough that you'll have an idea of what's going on, #proper naming (I hope!) 
This is my first real Android project so please do let me know if there are better ways of doing things :)
Below you'll find all my ramblings that I type up, these are things that I've implemented/thinking of implementing/ bugs and bug fixes.

Some clue into my intuition, the only component the activities should have to touch directly should be the Logic Engine, from there the logic engine decides what goes to where, keep reading on later for more insight into why this is.
Most requests that lead to a state change SHOULD be done through an order() command.
ALL requests for a variable should be STATIC if other components could need them, it free's up a get instance()
although when i started i wasn't quite committed to that as I hear static==bad typically but darn does it make things easier
Encapsulations isn't a huge deal as most things are package private or private that would really affect the game. It's shielded from the activities

However, I'm still a bit wary about it but it doesn't appear to negatively impact the game performance wise
And making some of them nonstatic breaks the logic as some have to be referenced from anon classes.
Again however, this may secretly be poison and it just hasn't hit yet 
But I've had it running both ways, one takes a lot of instancing and another is simple and uses about the same (maybe even less, recorded memory :P)
JUDGEMENT FOR NOW => most of the variables stay private static. If it becomes a problem I can easily swap everything out to instance based
I've included a copy of each instance to one another and getInstance() will always call the same one.
Anyways...
If you are looking for a place to hop in, this is it.

My Notes:

There is still a bug i have not been able to recreate, where on game over the user wont be able to reset I'll find it

Fixed time glitch where it would "jump times" after engine was paused, it now runs in realtime
Fixed time glitch where it ends >0 due to add time at last minute

in ADVANCED STAGE + Okays remove time (this is a balancing effort)
Also solved bug where timer can go negative if you happened to hit an okay at 0,1 time left

all greats/excellents  should have a chance to grant extra time, that decreases as you play
however the amount they award goes up

Greats need to give a certain amount of time at a probability that starts high and decreases among stages

Pause button has been materialized


Animations added
Score is dynamic now and changes as the user progress past various stages
Works by ranges (Levels) INTRO->BEGINNER->INTERMEDIATE->ADVANCED->MASTER
Each defines so-called conditions which give you a score from okay to excellent
Also have placeholders calling the Logic-UpdateLevel command, eventually the handler should be able to view the level
the logic engine is running under and update the user when it changes as this message is handled
Message-Based engines for the win! :D

Warp tap animation fades out much quicker now, so it's less confusing
EVENTUALLY CHANE WARP CLASS FROM IMAGEVIEW TO VIDEOPLAYER
User is now alerted to how well they did by an animation that occurs upon  a sucessful tap!

I'm now logging the x,y of every non successful tap for purposes that will be added in later.
One of those may even include a game mode that requires perfection >:) for the people that feel daring

Do plan on integrating Google Play Games Support, also will setup a server to store highscores :) 
(Likely SQL server, programmed in C# because <3 LINQ)

This is a message based sysem so you can find some helpful commands here:

Placeholder Keyword => Finish Later Ctrl+F This to find quick points of entry
Use this for when there is a feature placeholder. 

A typical order function looks like this 
component.order("OrderClass-command)

For now there are only two Order Classes

Game, for orders to the Logic Engine from an activity 
Logic, for orders to the Graphics Handler from the Logic engine
In the future messages to the SoundHandler will begin with Sound

Commands will always be preceeded b a "-" failure to do so will result in the components 
serve() method discarding your request.

You may notice that I've chosen outside requests to the Logic ENGINE to require a context
This is simply because many of it's functions pertain to a specific context and it's a mem leak to keep a global context ref.
Instead Its just better to pass around a single context reference and get rid of it when it's not needed.
I'd recommend if you need to access UI from another component's core thread. Pass it from the entry point off of the main loop
(WHICH SHOULD BE RUNNING IN THE LOGIC ENGINE) 
The entry point off of the main game loop for Graphics is simply update(context) update does a couple safeguard checks then responds to it's queue via the serve(context) funtion. 
Context is key, and without the proper context UI access is unachievable, to safeguard against this, gateway entrypoints to the logic engine (which should be the only part of the system that the activities touch directly) test the context to make sure it is a proper one

Going around this safeguard risks your code not working, or even breaking the game. 
Please only load context into Logic Engine. Then either modify an existing loop or make a new loop /w a corresponding mode
and use LogicEngine's switchMode(Mode,Context) to load into the proper loop and work from there.

Yes it's a headache but it makes it easy to keep track of context, we don't want that flying all over the place and safeguards allow
certain assumptions to be made that you will appreciate as you move high up the abstraction level. I know i did at least.

With all that said the two main functions to be aware of for adding order functionality, is the corresponding serve() method!
Any new functions can b implemented there.


NEW ORDER FUNCTIONS FOR LOGICENGINE
Game
	-Reset
	-Exit

NEW ORDER FUNCTIONS FOR GRAPHICS
Logic
	-UpdateTime
	-UpdateScore
	-UpdateLevel
	-UpdateStats (Updates pop count, etc.)
	-GameOver (initiates gameover)
