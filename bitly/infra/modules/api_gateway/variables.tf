variable "app_name" {
  type = string
}

variable "domain" {
  type = string
}

variable "certificate_arn" {
  type = string
}

variable "subnet_ids" {
  type = list(string)
}

variable "security_group_ids" {
  type = list(string)
}

variable "alb_listener_arn" {
  type = string
}
