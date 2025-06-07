package com.jbqneto.dev.botdev.service;

import com.jbqneto.dev.botdev.dto.ActivePositionDto;
import java.util.Optional;
import java.util.Set;

public interface PositionService {

    /**
     * Gets the active position for a given symbol.
     * @param symbol The trading symbol.
     * @return An Optional containing the ActivePositionDto if a position exists, otherwise empty.
     */
    Optional<ActivePositionDto> getPosition(String symbol);

    /**
     * Adds or updates an active position.
     * @param position The position to add or update.
     */
    void updatePosition(ActivePositionDto position);

    /**
     * Removes an active position for a given symbol.
     * @param symbol The trading symbol.
     */
    void removePosition(String symbol);

    /**
     * Gets all currently active symbols.
     * @return A set of symbols that have active positions.
     */
    Set<String> getAllActiveSymbols();

    /**
     * Checks if there is an active position for the given symbol.
     * @param symbol The trading symbol.
     * @return true if an active position exists, false otherwise.
     */
    boolean hasActivePosition(String symbol);
}
