package de.seuhd.campuscoffee.data.impl;

import de.seuhd.campuscoffee.domain.exceptions.OsmNodeNotFoundException;
import de.seuhd.campuscoffee.domain.model.CampusType;
import de.seuhd.campuscoffee.domain.model.OsmNode;
import de.seuhd.campuscoffee.domain.model.PosType;
import de.seuhd.campuscoffee.domain.ports.OsmDataService;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * OSM import service.
 */
@Service
@Slf4j
class OsmDataServiceImpl implements OsmDataService {

    private static final String OSM_NODE_URL = "https://www.openstreetmap.org/api/0.6/node/{id}";

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public @NonNull OsmNode fetchNode(@NonNull Long nodeId) throws OsmNodeNotFoundException {
        log.info("Fetching OSM node {} from {}", nodeId, OSM_NODE_URL);

        String xml;
        try {
            xml = restTemplate.getForObject(OSM_NODE_URL, String.class, nodeId);
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("OSM node {} not found (404)", nodeId);
            throw new OsmNodeNotFoundException(nodeId);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new OsmNodeNotFoundException(nodeId);
            }
            log.error("HTTP error while fetching OSM node {}: {}", nodeId, e.getMessage());
            throw new OsmNodeNotFoundException(nodeId);
        } catch (Exception e) {
            log.error("Unexpected error while fetching OSM node {}: {}", nodeId, e.getMessage());
            throw new OsmNodeNotFoundException(nodeId);
        }

        if (xml == null || xml.isBlank()) {
            throw new OsmNodeNotFoundException(nodeId);
        }

        try {
            Document doc = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
            doc.getDocumentElement().normalize();

            // Look for node element
            NodeList nodeElements = doc.getElementsByTagName("node");
            if (nodeElements.getLength() == 0) {
                throw new OsmNodeNotFoundException(nodeId);
            }
            Element nodeEl = (Element) nodeElements.item(0);

            // Parse tags
            NodeList tags = nodeEl.getElementsByTagName("tag");

            String name = null;
            String street = null;
            String houseNumber = null;
            Integer postalCode = null;
            String city = null;
            PosType posType = null;
            CampusType campus = null;

            for (int i = 0; i < tags.getLength(); i++) {
                Element tag = (Element) tags.item(i);
                String k = tag.getAttribute("k");
                String v = tag.getAttribute("v");
                if (k == null || v == null) continue;
                switch (k) {
                    case "name":
                        name = v;
                        break;
                    case "addr:street":
                        street = v;
                        break;
                    case "addr:housenumber":
                        houseNumber = v;
                        break;
                    case "addr:postcode":
                        try {
                            postalCode = Integer.parseInt(v.replaceAll("\\D", ""));
                        } catch (NumberFormatException ignored) {
                        }
                        break;
                    case "addr:city":
                        city = v;
                        break;
                    case "amenity":
                    case "shop":
                        try {
                            posType = PosType.valueOf(v.toUpperCase(Locale.ROOT));
                        } catch (IllegalArgumentException ignored) {
                        }
                        break;
                    case "campus":
                    case "seuhd:campus":
                        try {
                            campus = CampusType.valueOf(v.toUpperCase(Locale.ROOT));
                        } catch (IllegalArgumentException ignored) {
                        }
                        break;
                    default:
                        // ignore unknown tags
                }
            }

            return OsmNode.builder()
                    .nodeId(nodeId)
                    .name(name)
                    .street(street)
                    .houseNumber(houseNumber)
                    .postalCode(postalCode)
                    .city(city)
                    .type(posType)
                    .campus(campus)
                    .build();

        } catch (OsmNodeNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to parse OSM node XML for id {}: {}", nodeId, e.getMessage());
            throw new OsmNodeNotFoundException(nodeId);
        }
    }
}