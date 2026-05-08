package dev.nicktriano.bitly.meta;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MetaController {

  private final MetaService metaService;

  MetaController(MetaService metaService) {
    this.metaService = metaService;
  }

  @GetMapping("/meta")
  public MetaInfo getMeta() {
    return metaService.getMetaInfo();
  }
}
