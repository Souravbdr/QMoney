
package com.crio.warmup.stock.quotes;

import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.dto.TiingoCandle;
import com.crio.warmup.stock.exception.StockQuoteServiceException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import org.springframework.web.client.RestTemplate;

public class TiingoService implements StockQuotesService {

  private RestTemplate restTemplate;
  private ObjectMapper objectMapper = new ObjectMapper();

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

  public TiingoService(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  @Override
  public List<Candle> getStockQuote(String symbol, LocalDate from, LocalDate to) throws JsonProcessingException, StockQuoteServiceException{
    objectMapper.registerModule(new JavaTimeModule());
    String url = buildUri(symbol, from, to);
    String response ="";
    try{
      response = restTemplate.getForObject(url, String.class);
      if (response == null || response.isEmpty() || response.contains("Error") || !response.startsWith("[{")) {
        throw new StockQuoteServiceException("Response contains no data.");
      }
    } catch(RuntimeException e){
      throw new RuntimeException();
    }

    Candle[] candle = objectMapper.readValue(response, TiingoCandle[].class);
    Arrays.sort(candle,Comparator.comparing(Candle::getDate));
    return Arrays.asList(candle);
  }

  private String buildUri(String symbol, LocalDate startDate, LocalDate endDate) {
    String date = endDate.toString();
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
  

  // TODO: CRIO_TASK_MODULE_ADDITIONAL_REFACTOR
  //  Implement getStockQuote method below that was also declared in the interface.

  // Note:
  // 1. You can move the code from PortfolioManagerImpl#getStockQuote inside newly created method.
  // 2. Run the tests using command below and make sure it passes.
  //    ./gradlew test --tests TiingoServiceTest


  //CHECKSTYLE:OFF

  // TODO: CRIO_TASK_MODULE_ADDITIONAL_REFACTOR
  //  Write a method to create appropriate url to call the Tiingo API.





  // TODO: CRIO_TASK_MODULE_EXCEPTIONS
  //  1. Update the method signature to match the signature change in the interface.
  //     Start throwing new StockQuoteServiceException when you get some invalid response from
  //     Tiingo, or if Tiingo returns empty results for whatever reason, or you encounter
  //     a runtime exception during Json parsing.
  //  2. Make sure that the exception propagates all the way from
  //     PortfolioManager#calculateAnnualisedReturns so that the external user's of our API
  //     are able to explicitly handle this exception upfront.

  //CHECKSTYLE:OFF


}
