resource "random_password" "db" {
  length  = 32
  special = false
}

resource "aws_secretsmanager_secret" "db" {
  name = "${var.app_name}/db-v2"

  lifecycle {
    prevent_destroy = true
  }
}

resource "aws_secretsmanager_secret_version" "db" {
  secret_id = aws_secretsmanager_secret.db.id
  secret_string = jsonencode({
    username = var.db_username
    password = random_password.db.result
  })

  lifecycle {
    prevent_destroy = true
  }
}

resource "aws_db_subnet_group" "main" {
  name       = var.app_name
  subnet_ids = var.subnet_ids
}

resource "aws_db_instance" "main" {
  identifier        = var.app_name
  engine            = "postgres"
  engine_version    = "17"
  instance_class    = "db.t3.micro"
  allocated_storage = 20

  db_name  = var.db_name
  username = var.db_username
  password = random_password.db.result

  db_subnet_group_name   = aws_db_subnet_group.main.name
  vpc_security_group_ids = [var.security_group_id]

  multi_az            = true
  skip_final_snapshot = true
  deletion_protection = false
}
