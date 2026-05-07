output "ecr_repository_url" {
  value = aws_ecr_repository.app.repository_url
}

output "app_url" {
  value = "https://${var.domain}"
}

output "alb_dns_name" {
  value = aws_lb.main.dns_name
}
