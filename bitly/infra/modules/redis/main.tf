resource "aws_elasticache_parameter_group" "main" {
  name   = var.app_name
  family = "redis7"

  parameter {
    name  = "maxmemory-policy"
    value = "allkeys-lru"
  }
}

resource "aws_elasticache_subnet_group" "main" {
  name       = var.app_name
  subnet_ids = var.subnet_ids
}

resource "aws_elasticache_replication_group" "main" {
  replication_group_id       = var.app_name
  description                = "${var.app_name} redis"
  engine                     = "redis"
  engine_version             = "7.1"
  node_type                  = "cache.t4g.micro"
  num_cache_clusters         = 2
  parameter_group_name       = aws_elasticache_parameter_group.main.name
  subnet_group_name          = aws_elasticache_subnet_group.main.name
  security_group_ids         = [var.security_group_id]
  automatic_failover_enabled = true
  port                       = 6379
}
