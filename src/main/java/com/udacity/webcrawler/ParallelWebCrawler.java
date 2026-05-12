package com.udacity.webcrawler;

import com.udacity.webcrawler.parser.PageParser;
import com.udacity.webcrawler.parser.PageParserFactory;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RecursiveAction;
import java.util.regex.Pattern;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;

import com.udacity.webcrawler.json.CrawlResult;

import javax.inject.Inject;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

/**
 * A concrete implementation of {@link WebCrawler} that runs multiple threads on a
 * {@link ForkJoinPool} to fetch and process multiple web pages in parallel.
 */
final class ParallelWebCrawler implements WebCrawler {
  private final Clock clock;
  private final Duration timeout;
  private final int popularWordCount;
  private final ForkJoinPool pool;

  private final PageParserFactory parserFactory;
  private final int maxDepth;
  private final List<Pattern> ignoredUrls;

  @Inject
  ParallelWebCrawler(
          Clock clock,
          PageParserFactory parserFactory,
          @Timeout Duration timeout,
          @PopularWordCount int popularWordCount,
          @MaxDepth int maxDepth,
          @IgnoredUrls List<Pattern> ignoredUrls,
          @TargetParallelism int threadCount) {
    this.clock = clock;
    this.timeout = timeout;
    this.popularWordCount = popularWordCount;
    this.pool = new ForkJoinPool(Math.min(threadCount, getMaxParallelism()));

    this.parserFactory = parserFactory;
    this.maxDepth = maxDepth;
    this.ignoredUrls = ignoredUrls;
  }

  @Override
  public CrawlResult crawl(List<String> startingUrls) {

    Instant deadline = clock.instant().plus(timeout);

    Map<String, Integer> counts = new ConcurrentHashMap<>();

    Set<String> visitedUrls = ConcurrentHashMap.newKeySet();

    List<CrawlerTask> tasks = new ArrayList<>();

    for (String url : startingUrls) {

      tasks.add(
              new CrawlerTask(
                      url,
                      deadline,
                      maxDepth,
                      counts,
                      visitedUrls));
    }

    for (CrawlerTask task : tasks) {
      pool.invoke(task);
    }

    if (counts.isEmpty()) {

      return new CrawlResult.Builder()
              .setWordCounts(counts)
              .setUrlsVisited(visitedUrls.size())
              .build();
    }

    return new CrawlResult.Builder()
            .setWordCounts(WordCounts.sort(counts, popularWordCount))
            .setUrlsVisited(visitedUrls.size())
            .build();
  }
  private class CrawlerTask extends RecursiveAction {

    private final String url;
    private final Instant deadline;
    private final int depth;
    private final Map<String, Integer> counts;
    private final Set<String> visitedUrls;

    CrawlerTask(
            String url,
            Instant deadline,
            int depth,
            Map<String, Integer> counts,
            Set<String> visitedUrls) {

      this.url = url;
      this.deadline = deadline;
      this.depth = depth;
      this.counts = counts;
      this.visitedUrls = visitedUrls;
    }

    @Override
    protected void compute() {

      if (depth == 0 || clock.instant().isAfter(deadline)) {
        return;
      }

      for (Pattern pattern : ignoredUrls) {
        if (pattern.matcher(url).matches()) {
          return;
        }
      }

      if (!visitedUrls.add(url)) {
        return;
      }
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return;
      }
      PageParser.Result result = parserFactory.get(url).parse();

      for (Map.Entry<String, Integer> entry : result.getWordCounts().entrySet()) {

        counts.merge(entry.getKey(), entry.getValue(), Integer::sum);
      }
      List<CrawlerTask> tasks = new ArrayList<>();

      for (String link : result.getLinks()) {

        tasks.add(
                new CrawlerTask(
                        link,
                        deadline,
                        depth - 1,
                        counts,
                        visitedUrls));
      }

      invokeAll(tasks);
    }
  }
  @Override
  public int getMaxParallelism() {
    return Runtime.getRuntime().availableProcessors();
  }
}
