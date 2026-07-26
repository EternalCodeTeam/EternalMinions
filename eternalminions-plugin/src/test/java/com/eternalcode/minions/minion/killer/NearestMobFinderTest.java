package com.eternalcode.minions.minion.killer;

import static org.assertj.core.api.Assertions.assertThat;

import com.eternalcode.minions.minion.impl.killer.NearestMobFinder;
import java.util.List;
import org.junit.jupiter.api.Test;

class NearestMobFinderTest {

    private final NearestMobFinder finder = new NearestMobFinder();

    @Test
    void returnsMinusOneWhenNoCandidateIsAllowed() {
        List<NearestMobFinder.Candidate> candidates = List.of(
            new NearestMobFinder.Candidate(0, 1, 0, 0, false),
            new NearestMobFinder.Candidate(1, 2, 0, 0, false)
        );
        assertThat(this.finder.findNearest(0, 0, 0, candidates)).isEqualTo(-1);
    }

    @Test
    void returnsTheClosestAllowedCandidate() {
        List<NearestMobFinder.Candidate> candidates = List.of(
            new NearestMobFinder.Candidate(0, 5, 0, 0, true),
            new NearestMobFinder.Candidate(1, 1, 0, 0, true),
            new NearestMobFinder.Candidate(2, 3, 0, 0, true)
        );
        assertThat(this.finder.findNearest(0, 0, 0, candidates)).isEqualTo(1);
    }

    @Test
    void ignoresDisallowedCandidatesEvenWhenCloser() {
        List<NearestMobFinder.Candidate> candidates = List.of(
            new NearestMobFinder.Candidate(0, 1, 0, 0, false),
            new NearestMobFinder.Candidate(1, 5, 0, 0, true)
        );
        assertThat(this.finder.findNearest(0, 0, 0, candidates)).isEqualTo(1);
    }
}
