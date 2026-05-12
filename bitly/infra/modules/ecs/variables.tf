variable "app_name" {
  type = string
}

variable "region" {
  type = string
}

variable "image_url" {
  type = string
}

variable "db_url" {
  type = string
}

variable "db_secret_arn" {
  type = string
}

variable "subnet_ids" {
  type = list(string)
}

variable "security_group_id" {
  type = string
}

variable "target_group_arn" {
  type = string
}

