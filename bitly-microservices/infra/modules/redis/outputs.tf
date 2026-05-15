output "host" {
  value = aws_elasticache_replication_group.main.primary_endpoint_address
}
