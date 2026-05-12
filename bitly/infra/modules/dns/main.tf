data "aws_route53_zone" "main" {
  name = var.hosted_zone
}

resource "aws_route53_record" "cert_validation" {
  for_each = {
    for dvo in var.domain_validation_options : dvo.domain_name => {
      name   = dvo.resource_record_name
      type   = dvo.resource_record_type
      record = dvo.resource_record_value
    }
  }

  zone_id = data.aws_route53_zone.main.zone_id
  name    = each.value.name
  type    = each.value.type
  records = [each.value.record]
  ttl     = 60
}

resource "aws_acm_certificate_validation" "app" {
  certificate_arn         = var.certificate_arn
  validation_record_fqdns = [for r in aws_route53_record.cert_validation : r.fqdn]
}

resource "aws_route53_record" "app" {
  zone_id = data.aws_route53_zone.main.zone_id
  name    = var.domain
  type    = "A"

  alias {
    name                   = var.alias_dns_name
    zone_id                = var.alias_zone_id
    evaluate_target_health = true
  }
}
