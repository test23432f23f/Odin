package me.odinclient.utils.skyblock;

import java.util.TimerTask;

public class Timer
{
    private long timeMS = System.currentTimeMillis();

    public boolean hasPassed(long passedMS)
    {
        return (System.currentTimeMillis() - timeMS) >= passedMS;
    }

    public long getTimePassed()
    {
        return (System.currentTimeMillis() - timeMS);
    }

    public void reset()
    {
        timeMS = System.currentTimeMillis();
    }

    public static void schedule(Runnable r, long delay)
    {
        new java.util.Timer().schedule(new TimerTask()
        {
            @Override
            public void run()
            {
                r.run();
            }
        }, delay);
    }
}
