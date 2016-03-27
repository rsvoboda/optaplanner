/*
 * Copyright 2011 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.optaplanner.benchmark.impl.statistic.calculatecount;

import org.optaplanner.benchmark.config.statistic.ProblemStatisticType;
import org.optaplanner.benchmark.impl.result.SubSingleBenchmarkResult;
import org.optaplanner.benchmark.impl.statistic.ProblemBasedSubSingleStatistic;
import org.optaplanner.core.api.solver.Solver;
import org.optaplanner.core.impl.phase.event.PhaseLifecycleListenerAdapter;
import org.optaplanner.core.impl.phase.scope.AbstractStepScope;
import org.optaplanner.core.impl.score.definition.ScoreDefinition;
import org.optaplanner.core.impl.solver.DefaultSolver;
import org.optaplanner.core.impl.solver.scope.DefaultSolverScope;

import java.util.List;

public class CalculateCountSubSingleStatistic<Solution_>
        extends ProblemBasedSubSingleStatistic<Solution_, CalculateCountStatisticPoint> {

    private final long timeMillisThresholdInterval;

    private final CalculateCountSubSingleStatisticListener listener;

    public CalculateCountSubSingleStatistic(SubSingleBenchmarkResult subSingleBenchmarkResult) {
        this(subSingleBenchmarkResult, 1000L);
    }

    public CalculateCountSubSingleStatistic(SubSingleBenchmarkResult benchmarkResult, long timeMillisThresholdInterval) {
        super(benchmarkResult, ProblemStatisticType.CALCULATE_COUNT_PER_SECOND);
        if (timeMillisThresholdInterval <= 0L) {
            throw new IllegalArgumentException("The timeMillisThresholdInterval (" + timeMillisThresholdInterval
                    + ") must be bigger than 0.");
        }
        this.timeMillisThresholdInterval = timeMillisThresholdInterval;
        listener = new CalculateCountSubSingleStatisticListener();
    }

    // ************************************************************************
    // Lifecycle methods
    // ************************************************************************

    public void open(Solver<Solution_> solver) {
        ((DefaultSolver<Solution_>) solver).addPhaseLifecycleListener(listener);
    }

    public void close(Solver<Solution_> solver) {
        ((DefaultSolver<Solution_>) solver).removePhaseLifecycleListener(listener);
    }

    private class CalculateCountSubSingleStatisticListener extends PhaseLifecycleListenerAdapter<Solution_> {

        private long nextTimeMillisThreshold = timeMillisThresholdInterval;
        private long lastTimeMillisSpent = 0L;
        private long lastCalculateCount = 0L;

        @Override
        public void stepEnded(AbstractStepScope<Solution_> stepScope) {
            long timeMillisSpent = stepScope.getPhaseScope().calculateSolverTimeMillisSpent();
            if (timeMillisSpent >= nextTimeMillisThreshold) {
                DefaultSolverScope<Solution_> solverScope = stepScope.getPhaseScope().getSolverScope();
                long calculateCount = solverScope.getCalculateCount();
                long calculateCountInterval = calculateCount - lastCalculateCount;
                long timeMillisSpentInterval = timeMillisSpent - lastTimeMillisSpent;
                if (timeMillisSpentInterval == 0L) {
                    // Avoid divide by zero exception on a fast CPU
                    timeMillisSpentInterval = 1L;
                }
                long averageCalculateCountPerSecond = calculateCountInterval * 1000L / timeMillisSpentInterval;
                pointList.add(new CalculateCountStatisticPoint(timeMillisSpent, averageCalculateCountPerSecond));
                lastCalculateCount = calculateCount;

                lastTimeMillisSpent = timeMillisSpent;
                nextTimeMillisThreshold += timeMillisThresholdInterval;
                if (nextTimeMillisThreshold < timeMillisSpent) {
                    nextTimeMillisThreshold = timeMillisSpent;
                }
            }
        }

    }

    // ************************************************************************
    // CSV methods
    // ************************************************************************

    @Override
    protected String getCsvHeader() {
        return CalculateCountStatisticPoint.buildCsvLine("timeMillisSpent", "calculateCountPerSecond");
    }

    @Override
    protected CalculateCountStatisticPoint createPointFromCsvLine(ScoreDefinition scoreDefinition,
            List<String> csvLine) {
        return new CalculateCountStatisticPoint(Long.parseLong(csvLine.get(0)),
                Long.parseLong(csvLine.get(1)));
    }

}
