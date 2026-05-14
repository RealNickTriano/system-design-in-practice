variable "hosted_zone" {
  type = string
}

variable "domain" {
  type = string
}

variable "certificate_arn" {
  description = "Unvalidated ACM certificate ARN"
  type        = string
}

variable "domain_validation_options" {
  description = "domain_validation_options from the ACM certificate resource"
  type = set(object({
    domain_name           = string
    resource_record_name  = string
    resource_record_type  = string
    resource_record_value = string
  }))
}

variable "alias_dns_name" {
  description = "DNS name of the target resource to alias the domain to"
  type        = string
}

variable "alias_zone_id" {
  description = "Hosted zone ID of the target resource"
  type        = string
}
