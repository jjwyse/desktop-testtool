package com.jjw.testtool.camel;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.camel.AsyncCallback;
import org.apache.camel.AsyncProcessor;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.processor.loadbalancer.FailOverLoadBalancer;
import org.apache.camel.util.AsyncProcessorConverterHelper;
import org.apache.camel.util.AsyncProcessorHelper;
import org.apache.camel.util.ExchangeHelper;
import org.apache.log4j.Logger;

public class StickyLoadBalancer extends FailOverLoadBalancer
{
    /** Logger instance. */
    Logger LOG = Logger.getLogger(StickyLoadBalancer.class);

    /** The amount of time we should wait in between failing over. */
    private int myWaitTime;

    /** Number of attempts we make on an endpoint before failing over to another endpoint */
    private int myRetryAttempts;

    /** Keeps track of what endpoint we last used successfully */
    final AtomicInteger index = new AtomicInteger();

    /**
     * Primary constructor. This sets our wait time in between failover attempts to zero milliseconds.
     */
    public StickyLoadBalancer()
    {
        super();
        myWaitTime = 500;
        myRetryAttempts = 5;
    }

    /**
     * {@inheritDoc}
     * 
     * Stripped out any round robin code and added logic to use 'sticky failover'.
     */
    @Override
    public boolean process(Exchange exchange, AsyncCallback asyncCallback)
    {
        final List<Processor> processors = getProcessors();

        final AtomicInteger attempts = new AtomicInteger();
        boolean first = true;

        LOG.debug("Failover starting with endpoint index " + index);

        Exchange copy = null;
        while (first || shouldFailOver(copy))
        {
            if (!first)
            {
                LOG.warn("Failed on attempt " + attempts.get() + " to send message");
                attempts.incrementAndGet();
                if (attempts.get() > myRetryAttempts)
                {
                    LOG.info("Number of attempts: " + attempts.get() + " exceeds the number of retry attempts: "
                            + myRetryAttempts + ". Failing over to the next endpoint.");
                    index.incrementAndGet();
                    if (index.get() >= processors.size())
                    {
                        LOG.debug("Resetting index of endpoint to 0");
                        index.set(0);
                    }
                    attempts.set(0);
                }

                // Wait before moving on
                LOG.info("Waiting " + myWaitTime + " milliseconds before attempting to send message again");
                try
                {
                    Thread.sleep(myWaitTime);
                }
                catch (InterruptedException exception)
                {
                    LOG.error("Error while trying to sleep", exception);
                }
            }
            else
            {
                // flip first switch
                first = false;
            }

            // try again but copy original exchange before we failover
            copy = prepareExchangeForFailover(exchange);
            Processor processor = processors.get(index.get());

            // process the exchange
            boolean sync = processExchange(processor, exchange, copy, attempts, index, asyncCallback, processors);

            // continue as long its being processed synchronously
            if (!sync)
            {
                LOG.trace("Processing exchangeId: " + exchange.getExchangeId()
                        + " is continued being processed asynchronously");
                // the remainder of the failover will be completed async
                // so we break out now, then the callback will be invoked which then continue routing from where we left
                // here
                return false;
            }

            LOG.trace("Processing exchangeId: " + exchange.getExchangeId()
                    + " is continued being processed synchronously");
        }

        // and copy the current result to original so it will contain this result of this eip
        if (copy != null)
        {
            ExchangeHelper.copyResults(exchange, copy);
        }
        LOG.debug("Failover complete for exchangeId: " + exchange.getExchangeId() + " >>> " + exchange);
        asyncCallback.done(true);
        return true;
    }

    /**
     * Taken from Apache Camel 2.9.2 -- FailOverLoadBalancer processExchange
     * 
     * @param processor
     * @param exchange
     * @param copy
     * @param attempts
     * @param index
     * @param callback
     * @param processors
     * @return
     */
    private boolean processExchange(Processor processor, Exchange exchange, Exchange copy, AtomicInteger attempts,
            AtomicInteger index, AsyncCallback callback, List<Processor> processors)
    {
        if (processor == null)
        {
            throw new IllegalStateException("No processors could be chosen to process " + copy);
        }
        LOG.debug("Processing failover at attempt " + attempts + " for " + copy);

        AsyncProcessor albp = AsyncProcessorConverterHelper.convert(processor);
        return AsyncProcessorHelper.process(albp, copy, new FailOverAsyncCallback(exchange, copy, attempts, index,
                callback, processors));
    }

    /**
     * @param waitTime
     *            the waitTime to set
     */
    public void setWaitTime(int waitTime)
    {
        myWaitTime = waitTime;
    }

    /**
     * @param retryAttempts
     *            the retryAttempts to set
     */
    public void setRetryAttempts(int retryAttempts)
    {
        myRetryAttempts = retryAttempts;
    }

    /**
     * Taken from Apache Camel 2.9.2 -- FailOverLoadBalancer.FailOverAsyncCallback
     */
    private final class FailOverAsyncCallback implements AsyncCallback
    {

        private final Exchange exchange;
        private Exchange copy;
        private final AtomicInteger attempts;
        private final AtomicInteger index;
        private final AsyncCallback callback;
        private final List<Processor> processors;

        private FailOverAsyncCallback(Exchange exchange, Exchange copy, AtomicInteger attempts, AtomicInteger index,
                AsyncCallback callback, List<Processor> processors)
        {
            this.exchange = exchange;
            this.copy = copy;
            this.attempts = attempts;
            this.index = index;
            this.callback = callback;
            this.processors = processors;
        }

        @Override
        public void done(boolean doneSync)
        {
            // we only have to handle async completion of the pipeline
            if (doneSync)
            {
                return;
            }

            while (shouldFailOver(copy))
            {
                LOG.warn("Failed on attempt " + attempts.get() + " to send message");
                attempts.incrementAndGet();
                if (attempts.get() > myRetryAttempts)
                {
                    LOG.info("Number of attempts: " + attempts.get() + " exceeds the number of retry attempts: "
                            + myRetryAttempts + ". Failing over to the next endpoint.");
                    index.incrementAndGet();
                    if (index.get() >= processors.size())
                    {
                        LOG.debug("Resetting index of endpoint to 0");
                        index.set(0);
                    }
                    attempts.set(0);
                }

                // Wait before moving on
                LOG.info("Waiting " + myWaitTime + " milliseconds before attempting to send message again");
                try
                {
                    Thread.sleep(myWaitTime);
                }
                catch (InterruptedException exception)
                {
                    LOG.error("Error while trying to sleep", exception);
                }

                // try again but prepare exchange before we failover
                copy = prepareExchangeForFailover(exchange);
                Processor processor = processors.get(index.get());

                // try to failover using the next processor
                doneSync = processExchange(processor, exchange, copy, attempts, index, callback, processors);
                if (!doneSync)
                {
                    LOG.trace("Processing exchangeId: " + exchange.getExchangeId()
                            + " is continued being processed asynchronously");
                    // the remainder of the failover will be completed async
                    // so we break out now, then the callback will be invoked which then continue routing from where we
                    // left here
                    return;
                }
            }

            // and copy the current result to original so it will contain this result of this eip
            if (copy != null)
            {
                ExchangeHelper.copyResults(exchange, copy);
            }
            LOG.debug("Failover complete for exchangeId: " + exchange.getExchangeId() + " >>> " + exchange);
            // signal callback we are done
            callback.done(false);
        };
    }
}
