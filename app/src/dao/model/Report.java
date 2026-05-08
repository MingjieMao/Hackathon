package dao.model;


import sorteddata.SortedData;


import java.util.UUID;


public class Report implements HasUUID{

    public final UUID message;
    public final UUID user;
    public final long timestamp;


    @Override
    public UUID getUUID() {
        return null;
    }


    public Report(UUID message, UUID user, long timestamp){
        this.message = message;
        this.user = user;
        this.timestamp = timestamp;
    }
}
