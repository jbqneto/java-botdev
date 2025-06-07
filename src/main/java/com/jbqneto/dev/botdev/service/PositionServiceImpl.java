package com.jbqneto.dev.botdev.service;

import com.jbqneto.dev.botdev.dto.ActivePositionDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PositionServiceImpl implements PositionService {

    private static final Logger logger = LoggerFactory.getLogger(PositionServiceImpl.class);
    private final Map<String, ActivePositionDto> activePositions = new ConcurrentHashMap<>();

    @Override
    public Optional<ActivePositionDto> getPosition(String symbol) {
        return Optional.ofNullable(activePositions.get(symbol));
    }

    @Override
    public void updatePosition(ActivePositionDto position) {
        if (position == null || position.getSymbol() == null || position.getSymbol().isEmpty()) {
            logger.warn("Attempted to update position with null or invalid symbol.");
            return;
        }
        activePositions.put(position.getSymbol(), position);
        logger.info("Position updated/added: {}", position);
    }

    @Override
    public void removePosition(String symbol) {
        if (symbol == null || symbol.isEmpty()) {
            logger.warn("Attempted to remove position with null or invalid symbol.");
            return;
        }
        ActivePositionDto removedPosition = activePositions.remove(symbol);
        if (removedPosition != null) {
            logger.info("Position removed for symbol {}: {}", symbol, removedPosition);
        } else {
            logger.info("No active position found to remove for symbol: {}", symbol);
        }
    }

    @Override
    public Set<String> getAllActiveSymbols() {
        return activePositions.keySet();
    }

    @Override
    public boolean hasActivePosition(String symbol) {
        return activePositions.containsKey(symbol);
    }
}
