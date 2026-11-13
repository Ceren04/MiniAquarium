package com.miniakvaryum.domain.feature.tasks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.miniakvaryum.domain.core.save.SaveData;
import com.miniakvaryum.domain.core.save.SaveSection;
import com.miniakvaryum.domain.core.session.GameSession;
import com.miniakvaryum.domain.core.time.TimeAnchor;
import com.miniakvaryum.domain.feature.economy.EconomyConfig;
import com.miniakvaryum.domain.feature.economy.EconomyFeature;
import com.miniakvaryum.domain.feature.security.SecurityConfig;
import com.miniakvaryum.domain.feature.security.SecurityFeature;
import com.miniakvaryum.domain.feature.tasks.api.Task;
import com.miniakvaryum.domain.feature.tasks.api.TaskService;
import com.miniakvaryum.domain.feature.tasks.api.TaskStatus;
import com.miniakvaryum.domain.testsupport.TestGame;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Tests the wiring of the feature (install, save, load). The task rules are tested in TaskServiceTest. */
class TasksFeatureTest {

    private final TestGame game = new TestGame();

    private GameSession start() {
        return game.start(new EconomyFeature(EconomyConfig.defaults()),
                new SecurityFeature(SecurityConfig.defaults()),
                new TasksFeature(TasksConfig.defaults()));
    }

    @Test
    void newGameHasNoTasks() {
        assertThat(start().get(TaskService.class).list()).isEmpty();
    }

    @Test
    void savedTasksAreLoadedAndSavedBackUnchanged() {
        JsonElement data = JsonParser.parseString("""
                {"tasks":[
                  {"id":"7f3c","title":"Tidy your room","coinReward":10,"foodReward":2,"status":"APPROVED"},
                  {"id":"9a1b","title":"Do homework","coinReward":5,"foodReward":0,"status":"PENDING"}]}
                """);
        game.store.save(SaveData.of(new TimeAnchor(0, game.clock.monotonicNanos(), game.clock.wallMillis(), "boot-A"),
                Map.of("tasks", new SaveSection(1, data))));

        GameSession session = start();
        TaskService tasks = session.get(TaskService.class);

        assertThat(tasks.list()).containsExactly(
                new Task("7f3c", "Tidy your room", 10, 2, TaskStatus.APPROVED),
                new Task("9a1b", "Do homework", 5, 0, TaskStatus.PENDING));
        assertThat(tasks.list(TaskStatus.PENDING)).extracting(Task::id).containsExactly("9a1b");
        assertThat(tasks.list(TaskStatus.ACTIVE)).isEmpty();

        session.saveNow();
        assertThat(game.store.lastSaved().sections().get("tasks").data()).isEqualTo(data);
    }

    @Test
    void featureNeedsEconomyAndSecurityBeforeIt() {
        assertThatThrownBy(() -> game.start(new TasksFeature(TasksConfig.defaults())))
                .isInstanceOf(IllegalStateException.class);
    }
}
