import java.util.*;

class Team {
    final String name;
    final int points;
    final int goalDiff;
    Team(String name,int points,int goalDiff) {
        this.name=name; this.points=points; this.goalDiff=goalDiff;
    }
    @Override public String toString() { return name+"("+points+","+goalDiff+")"; }
}
class Leaderboard<T> {
    private final List<T> items=new ArrayList<>();
    private final Comparator<? super T> order;
    Leaderboard(Comparator<? super T> order) { this.order=Objects.requireNonNull(order); }
    void add(T item) { items.add(Objects.requireNonNull(item)); }
    void addAll(List<? extends T> source) { items.addAll(source); }
    List<T> ranking() {
        List<T> copy=new ArrayList<>(items);
        copy.sort(order);
        return copy;
    }
}
public class Q5_GenericLeaderboard {
    public static void main(String[] args) {
        Comparator<Team> ranking=Comparator.comparingInt((Team t)->t.points).reversed()
            .thenComparing(Comparator.comparingInt((Team t)->t.goalDiff).reversed())
            .thenComparing(t->t.name);
        Leaderboard<Team> board=new Leaderboard<>(ranking);
        board.addAll(List.of(
            new Team("Falcons",20,12),
            new Team("Tigers",20,12),
            new Team("Eagles",18,15),
            new Team("Wolves",20,8),
            new Team("Bulls",18,15)
        ));
        System.out.println("Final ranking:");
        board.ranking().forEach(System.out::println);
    }
}
