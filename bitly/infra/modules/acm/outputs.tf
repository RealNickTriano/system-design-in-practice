output "certificate_arn" {
  value = aws_acm_certificate.app.arn
}

output "domain_validation_options" {
  value = aws_acm_certificate.app.domain_validation_options
}
