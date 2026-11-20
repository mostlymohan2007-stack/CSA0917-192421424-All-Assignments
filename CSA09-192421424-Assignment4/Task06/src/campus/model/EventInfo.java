package campus.model;

/** An event students can register for. toString() is what a combo box displays. */
public final class EventInfo {
    private final int eventId;
    private final String title, venue, date;
    private final int capacity;

    public EventInfo(int eventId, String title, String venue, String date, int capacity) {
        this.eventId = eventId; this.title = title; this.venue = venue; this.date = date; this.capacity = capacity;
    }
    public int getEventId() { return eventId; }
    public String getTitle() { return title; }
    public String getVenue() { return venue; }
    public String getDate() { return date; }
    public int getCapacity() { return capacity; }
    @Override public String toString() { return title; }
}
