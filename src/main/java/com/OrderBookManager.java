package com;

import com.model.TickEvent;
import com.model.TickEventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

public class OrderBookManager implements TickEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    public void onEvent(TickEvent tickEvent, long l, boolean b) {
        LOGGER.info("TickEvent received <{}> <{}> <{}>", tickEvent, l, b);
    }
}
