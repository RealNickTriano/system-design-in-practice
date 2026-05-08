package dev.nicktriano.bitly.meta;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class MetaService {

  private static final String IMDS_BASE = "http://169.254.169.254/latest";
  private static final String TOKEN_TTL = "21600";

  private final RestClient restClient = RestClient.create();

  public MetaInfo getMetaInfo() {
    try {
      String token = restClient.put()
          .uri(IMDS_BASE + "/api/token")
          .header("X-aws-ec2-metadata-token-ttl-seconds", TOKEN_TTL)
          .retrieve()
          .body(String.class);

      String region = restClient.get()
          .uri(IMDS_BASE + "/meta-data/placement/region")
          .header("X-aws-ec2-metadata-token", token)
          .retrieve()
          .body(String.class);

      String az = restClient.get()
          .uri(IMDS_BASE + "/meta-data/placement/availability-zone")
          .header("X-aws-ec2-metadata-token", token)
          .retrieve()
          .body(String.class);

      return new MetaInfo(region, az);
    } catch (Exception e) {
      return new MetaInfo(null, null);
    }
  }
}
