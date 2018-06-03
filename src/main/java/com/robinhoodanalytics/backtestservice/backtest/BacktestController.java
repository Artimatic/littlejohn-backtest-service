package com.robinhoodanalytics.backtestservice.backtest;

import com.robinhoodanalytics.backtestservice.BacktestServiceApplication;
import com.robinhoodanalytics.backtestservice.models.Signal;
import com.robinhoodanalytics.backtestservice.trainer.TrainerService;
import com.robinhoodanalytics.backtestservice.models.Quote;
import com.robinhoodanalytics.backtestservice.quotes.QuoteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/backtest")
public class BacktestController {
    @Autowired
    private QuoteService _quoteService;

    @Autowired
    private BacktestMainService _backtestMainService;

    @Autowired
    private TrainerService _trainService;

    private static final Logger log = LoggerFactory.getLogger(BacktestServiceApplication.class);

    @RequestMapping(method = RequestMethod.POST)
    ResponseEntity<List<Quote>> backtest(@RequestParam(value = "ticker") String symbol,
                                         @RequestParam @DateTimeFormat(pattern="yyyy-MM-dd") Date from,
                                         @RequestParam @DateTimeFormat(pattern="yyyy-MM-dd") Date to)
    {
        try {
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(MediaType.APPLICATION_JSON);
            List<Quote> quotes = _quoteService.getHistoricalQuotes(symbol, from, to);

            if (quotes != null) {
                return new ResponseEntity<>(quotes, responseHeaders, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } catch (RestClientException e) {
            return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    @RequestMapping(
            value = "/train",
            method = RequestMethod.GET)
    ResponseEntity<List<Quote>> train(@RequestParam(value = "ticker") String symbol,
                                         @RequestParam @DateTimeFormat(pattern="yyyy-MM-dd") Date from,
                                         @RequestParam @DateTimeFormat(pattern="yyyy-MM-dd") Date to)
    {
        try {
            return _trainService.train(symbol, from, to);
        } catch (RestClientException e) {
            return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    @RequestMapping(
            value = "/run",
            method = RequestMethod.POST)
    @ResponseBody
    public String executeSomeBacktest(@RequestParam("tickers") List<String> symbols,
                                      @RequestParam("strategy") String tradingStrategy,
                                      @RequestParam("cash") BigDecimal initialFund,
                                      @RequestParam @DateTimeFormat(pattern="yyyy-MM-dd") Date from,
                                      @RequestParam @DateTimeFormat(pattern="yyyy-MM-dd") Date to) 
    {
        switch(tradingStrategy.toLowerCase()) {
            case "buyandhold":
                _backtestMainService.buyAndHold(symbols, from, to, initialFund);
            break;
        }
        return "Get some Foos with Header";
    }

    @RequestMapping(
            value = "/strategy/mean-reversion/train",
            method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity meanReversionTrainer(@RequestParam("symbol") String symbol,
                                                                        @RequestParam @DateTimeFormat(pattern="yyyy-MM-dd") Date from,
                                                                        @RequestParam @DateTimeFormat(pattern="yyyy-MM-dd") Date to,
                                                                        @RequestParam(value = "d", required = false) BigDecimal deviation,
                                                                        @RequestParam("s") int shortTerm,
                                                                        @RequestParam("l") int longTerm

    ) {

        try {
            return ResponseEntity.ok(_backtestMainService.getMeanReversionResults(symbol, from, to, deviation, shortTerm, longTerm));
        } catch (Exception e) {
            log.error("Error: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @RequestMapping(
            value = "/strategy/mean-reversion/chart",
            method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity meanReversionChart(@RequestParam("symbol") String symbol,
                                             @RequestParam @DateTimeFormat(pattern="yyyy-MM-dd") Date from,
                                             @RequestParam @DateTimeFormat(pattern="yyyy-MM-dd") Date to,
                                             @RequestParam("s") int shortTerm,
                                             @RequestParam("l") int longTerm

    ) {

        try {
            return ResponseEntity.ok(_backtestMainService.getMeanReversionTimeline(symbol, from, to, shortTerm, longTerm));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e);
        }
    }
}
