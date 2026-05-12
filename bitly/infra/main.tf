terraform {
  required_version = "1.15.2"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "6.44.0"
    }
    random = {
      source  = "hashicorp/random"
      version = "3.8.1"
    }
  }

  backend "s3" {
    bucket       = "nicktriano-terraform-state"
    key          = "shorten/terraform.tfstate"
    region       = "us-east-1"
    encrypt      = true
    use_lockfile = true
  }
}

provider "aws" {
  region = var.region
}

# ── Networking ────────────────────────────────────────────────────────────────

module "vpc" {
  source = "./modules/vpc"

  app_name = var.app_name
}

# ── Certificate & DNS ─────────────────────────────────────────────────────────

module "acm" {
  source = "./modules/acm"

  domain = var.domain
}

module "dns" {
  source = "./modules/dns"

  hosted_zone               = var.hosted_zone
  domain                    = var.domain
  certificate_arn           = module.acm.certificate_arn
  domain_validation_options = module.acm.domain_validation_options
  alias_dns_name            = module.api_gateway.domain_name
  alias_zone_id             = module.api_gateway.hosted_zone_id
}

# ── Load balancer ─────────────────────────────────────────────────────────────

module "alb" {
  source = "./modules/alb"

  app_name          = var.app_name
  vpc_id            = module.vpc.vpc_id
  subnet_ids        = module.vpc.private_subnet_ids
  security_group_id = module.vpc.alb_security_group_id
}

# ── API Gateway ───────────────────────────────────────────────────────────────

module "api_gateway" {
  source = "./modules/api_gateway"

  app_name           = var.app_name
  domain             = var.domain
  certificate_arn    = module.dns.validated_certificate_arn
  subnet_ids         = module.vpc.private_subnet_ids
  security_group_ids = [module.vpc.vpc_link_security_group_id]
  alb_listener_arn   = module.alb.listener_arn
}

# ── Database ──────────────────────────────────────────────────────────────────

module "db" {
  source = "./modules/db"

  app_name          = var.app_name
  db_name           = var.db_name
  db_username       = var.db_username
  subnet_ids        = module.vpc.private_subnet_ids
  security_group_id = module.vpc.rds_security_group_id
}

# ── Compute ───────────────────────────────────────────────────────────────────

module "ecr" {
  source = "./modules/ecr"

  app_name = var.app_name
}

module "ecs" {
  source = "./modules/ecs"

  app_name          = var.app_name
  region            = var.region
  image_url         = "${module.ecr.repository_url}:${var.image_tag}"
  db_url            = "jdbc:postgresql://${module.db.endpoint}/${var.db_name}"
  db_secret_arn     = module.db.secret_arn
  subnet_ids        = module.vpc.private_subnet_ids
  security_group_id = module.vpc.ecs_security_group_id
  target_group_arn  = module.alb.target_group_arn

  depends_on = [module.alb]
}
