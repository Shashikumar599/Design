package pubsub.event;

import pubsub.topic.Topic;

public class ScoreEvent implements Event {
    int score;

    public ScoreEvent(int score){
        this.score = score;
    }

    public int getCurrScore() {
        return score;
    }
}
