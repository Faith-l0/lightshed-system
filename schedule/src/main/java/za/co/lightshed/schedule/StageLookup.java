package za.co.lightshed.schedule;

/** Asks the Stage service for the current loadshedding stage. */
public interface StageLookup {
    int currentStage() throws ServiceUnavailableException;
}
