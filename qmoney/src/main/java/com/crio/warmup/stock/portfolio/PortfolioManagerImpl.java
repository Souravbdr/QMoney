
package com.crio.warmup.stock.portfolio;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.SECONDS;

import com.crio.warmup.stock.dto.AnnualizedReturn;
import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.dto.PortfolioTrade;
import com.crio.warmup.stock.dto.TiingoCandle;
import com.crio.warmup.stock.exception.StockQuoteServiceException;
import com.crio.warmup.stock.quotes.StockQuotesService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.springframework.web.client.RestTemplate;

public class PortfolioManagerImpl implements PortfolioManager {

  private RestTemplate restTemplate;
  private StockQuotesService stockService;

  private static final String API = "https://api.tiingo.com/tiingo/daily/";
  private static final String SLASH = "/";
  private static final String PRICES = "prices";
  private static final String QUERY = "?";
  private static final String START_DATE = "startDate";
  private static final String EQUALS_SYMBOL = "=";
  private static final String END_DATE = "endDate";
  private static final String AMPERSANT = "&";
  private static final String TOKEN = "token";
  private static final String API_KEY = "53dd129a0ded2f9a4a865156b9937388a669a713";

  // Caution: Do not delete or modify the constructor, or else your build will break!
  // This is absolutely necessary for backward compatibility
  protected PortfolioManagerImpl(StockQuotesService stockService) {
    this.stockService = stockService;
  }

  protected PortfolioManagerImpl(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  private Comparator<AnnualizedReturn> getComparator() {
    return Comparator.comparing(AnnualizedReturn::getAnnualizedReturn).reversed();
  }

  // CHECKSTYLE:OFF
  public List<Candle> getStockQuote(String symbol, LocalDate from, LocalDate to)
      throws JsonProcessingException {
    String url = buildUri(symbol, from, to);
    Candle[] candle = restTemplate.getForObject(url, TiingoCandle[].class);
    return Arrays.asList(candle);
  }
  // TODO: CRIO_TASK_MODULE_REFACTOR
  // Extract the logic to call Tiingo third-party APIs to a separate function.
  // Remember to fill out the buildUri function and use that.

  protected String buildUri(String symbol, LocalDate startDate, LocalDate endDate) {
    String date = endDate.toString();
    // System.out.println("date==>"+date);
    StringBuilder urlBuilder = new StringBuilder(API);
    urlBuilder.append(symbol);
    urlBuilder.append(SLASH);
    urlBuilder.append(PRICES);
    urlBuilder.append(QUERY);
    urlBuilder.append(START_DATE);
    urlBuilder.append(EQUALS_SYMBOL);
    urlBuilder.append(startDate);
    urlBuilder.append(AMPERSANT);
    urlBuilder.append(END_DATE);
    urlBuilder.append(EQUALS_SYMBOL);
    urlBuilder.append(date);
    urlBuilder.append(AMPERSANT);
    urlBuilder.append(TOKEN);
    urlBuilder.append(EQUALS_SYMBOL);
    urlBuilder.append(API_KEY);
    return urlBuilder.toString();
  }

  protected static void sortCandles(List<Candle> candles) {
    Collections.sort(candles, (a, b) -> a.getDate().compareTo(b.getDate()));
  }

  protected Double getOpeningPriceOnStartDate(List<Candle> candles) {
    sortCandles(candles);
    return candles.get(0).getOpen();
  }

  protected static Double getClosingPriceOnEndDate(List<Candle> candles) {
    sortCandles(candles);
    return candles.get(candles.size() - 1).getClose();
  }

  protected static AnnualizedReturn calculateAnnualizedReturns(LocalDate endDate,
      PortfolioTrade trade, Double buyPrice, Double sellPrice) {
    Double totalReturn = (sellPrice - buyPrice) / buyPrice;
    long holdingPeriodInDays = ChronoUnit.DAYS.between(trade.getPurchaseDate(), endDate);
    double holdingPeriodInYears = (double) holdingPeriodInDays / (double) 365;

    Double anualReturn = Math.pow((1 + totalReturn), (1 / holdingPeriodInYears)) - 1;
    return new AnnualizedReturn(trade.getSymbol(), anualReturn, totalReturn);
  }

  @Override
  public List<AnnualizedReturn> calculateAnnualizedReturn(List<PortfolioTrade> portfolioTrades,
      LocalDate endDate) throws StockQuoteServiceException {

    List<AnnualizedReturn> annualizedReturns = new ArrayList<>();

    for (PortfolioTrade trade : portfolioTrades) {
      List<Candle> candles = Collections.emptyList();
      try {
        candles = stockService.getStockQuote(trade.getSymbol(), trade.getPurchaseDate(), endDate);
      } catch (JsonProcessingException e) {
        e.printStackTrace();
      }
      Double buyPrice = getOpeningPriceOnStartDate(candles);
      Double sellPrice = getClosingPriceOnEndDate(candles);
      annualizedReturns.add(calculateAnnualizedReturns(endDate, trade, buyPrice, sellPrice));
    }
    Collections.sort(annualizedReturns, getComparator());
    return annualizedReturns;



    // Caution: Do not delete or modify the constructor, or else your build will break!
    // This is absolutely necessary for backward compatibility



    // ¶TODO: CRIO_TASK_MODULE_ADDITIONAL_REFACTOR
    // Modify the function #getStockQuote and start delegating to calls to
    // stockQuoteService provided via newly added constructor of the class.
    // You also have a liberty to completely get rid of that function itself, however, make sure
    // that you do not delete the #getStockQuote function.

  }

  @Override
  public List<AnnualizedReturn> calculateAnnualizedReturnParallel(
      List<PortfolioTrade> portfolioTrades, LocalDate endDate, int numThreads)
      throws InterruptedException, StockQuoteServiceException {
      ExecutorService executorService = Executors.newFixedThreadPool(numThreads);

    // TODO Auto-generated method stub
    List<AnnualizedReturn> annualizedReturns = new ArrayList<>();
    Map<Future<List<Candle>>,PortfolioTrade> futureCandleList = new HashMap<>();

    for (PortfolioTrade trade : portfolioTrades) {
      Callable<List<Candle>> callableTask = () -> {
        return stockService.getStockQuote(trade.getSymbol(),trade.getPurchaseDate(), endDate);
      };
      futureCandleList.put(executorService.submit(callableTask),trade);
    }
    for(Map.Entry<Future<List<Candle>>,PortfolioTrade> futureCandles:futureCandleList.entrySet()){
      List<Candle> candles = Collections.emptyList();
      try {
        candles = futureCandles.getKey().get();
        Double buyPrice = getOpeningPriceOnStartDate(candles);
        Double sellPrice = getClosingPriceOnEndDate(candles);
        annualizedReturns.add(calculateAnnualizedReturns(endDate, futureCandles.getValue(), buyPrice, sellPrice));
      } catch (ExecutionException e) {
        throw new StockQuoteServiceException("cant get future");
      }
    }
    Collections.sort(annualizedReturns, getComparator());
    return annualizedReturns;
  }
}
