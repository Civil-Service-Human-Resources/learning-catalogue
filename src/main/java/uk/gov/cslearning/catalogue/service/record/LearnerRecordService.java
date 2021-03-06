package uk.gov.cslearning.catalogue.service.record;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import uk.gov.cslearning.catalogue.domain.module.CancellationReason;
import uk.gov.cslearning.catalogue.domain.module.EventStatus;
import uk.gov.cslearning.catalogue.service.record.model.Booking;
import uk.gov.cslearning.catalogue.service.record.model.Event;

import java.util.List;

@Service
public class LearnerRecordService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LearnerRecordService.class);

    private final RestTemplate restTemplate;

    private final RequestEntityFactory requestEntityFactory;

    private final String eventUrlFormat;

    private final String bookingUrlFormat;

    public LearnerRecordService(RestTemplate restTemplate, RequestEntityFactory requestEntityFactory, @Value("${record.eventUrlFormat}") String eventUrlFormat, @Value("${record.bookingUrlFormat}") String bookingUrlFormat){
        this.restTemplate = restTemplate;
        this.requestEntityFactory = requestEntityFactory;
        this.eventUrlFormat = eventUrlFormat;
        this.bookingUrlFormat = bookingUrlFormat;
    }

    public List<Booking> getEventBookings(String eventId) {
        try{
            RequestEntity requestEntity = requestEntityFactory.createGetRequest(String.format(bookingUrlFormat, eventId));
            ResponseEntity<List<Booking>> responseEntity = restTemplate.exchange(requestEntity, new ParameterizedTypeReference<List<Booking>>(){});
            return responseEntity.getBody();
        } catch (RequestEntityException | RestClientException e) {
            LOGGER.error("Could not get bookings from learner record: ", e.getLocalizedMessage());
            return null;
        }
    }

    public EventStatus getEventStatus(String eventId) {
        try{
            RequestEntity requestEntity = requestEntityFactory.createGetRequest(String.format(eventUrlFormat, eventId));
            ResponseEntity<Event> responseEntity = restTemplate.exchange(requestEntity, Event.class);

            Event event = responseEntity.getBody();
            return EventStatus.forValue(event.getStatus());
        } catch (RequestEntityException | RestClientException e) {
            LOGGER.error("Could not get event from learner record: ", e.getLocalizedMessage());
            return null;
        }
    }

    public CancellationReason getCancellationReason(String eventId) {
        try{
            RequestEntity requestEntity = requestEntityFactory.createGetRequest(String.format(eventUrlFormat, eventId));
            ResponseEntity<Event> responseEntity = restTemplate.exchange(requestEntity, Event.class);

            Event event = responseEntity.getBody();
            return CancellationReason.forValue(event.getCancellationReason());
        } catch (RequestEntityException | RestClientException e) {
            LOGGER.error("Could not get event from learner record: ", e.getLocalizedMessage());
            return null;
        }
    }
}
