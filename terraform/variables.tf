variable "aws_region" {
  type        = string
  default     = "us-east-1"
  description = "AWS region for all resources."
}

variable "project_name" {
  type        = string
  default     = "nexus"
  description = "Base project name used in AWS resource names."
}

variable "environment" {
  type        = string
  default     = "prod"
  description = "Deployment environment suffix."
}

variable "instance_type" {
  type        = string
  default     = "t2.micro"
  description = "Elastic Beanstalk EC2 instance type."
}

variable "vpc_id" {
  type        = string
  default     = ""
  description = "Existing VPC id to deploy into. Leave empty to use the AWS default VPC."
}

variable "subnet_ids" {
  type        = list(string)
  default     = []
  description = "Existing subnet ids to deploy into. Leave empty to use the subnets from the selected/default VPC."
}

variable "eb_artifact_retention_days" {
  type        = number
  default     = 30
  description = "How long deploy bundles stay in the Elastic Beanstalk artifact bucket."
}

variable "db_name" {
  type        = string
  default     = "nexuschatbot_db"
  description = "PostgreSQL database name."
}

variable "db_username" {
  type        = string
  description = "PostgreSQL master username."
  sensitive   = true
}

variable "db_password" {
  type        = string
  description = "PostgreSQL master password."
  sensitive   = true
}

variable "db_instance_class" {
  type        = string
  default     = "db.t3.micro"
  description = "RDS PostgreSQL instance class."
}

variable "db_engine_version" {
  type        = string
  default     = "16.4"
  description = "RDS PostgreSQL engine version."
}

variable "db_allocated_storage" {
  type        = number
  default     = 20
  description = "Initial storage size in GB for PostgreSQL."
}

variable "db_max_allocated_storage" {
  type        = number
  default     = 100
  description = "Maximum autoscaled storage size in GB for PostgreSQL."
}

variable "db_backup_retention_period" {
  type        = number
  default     = 7
  description = "How many days of automated backups to keep."
}

variable "skip_final_snapshot" {
  type        = bool
  default     = true
  description = "Skip final snapshot on destroy. Keep true for disposable dev/prod bootstrap and false for safer teardown."
}

variable "initial_admin_username" {
  type        = string
  description = "Initial admin username used by the backend seed."
  sensitive   = true
}

variable "initial_admin_password" {
  type        = string
  description = "Initial admin password used by the backend seed."
  sensitive   = true
}

variable "jwt_secret" {
  type        = string
  description = "JWT secret injected into Elastic Beanstalk."
  sensitive   = true
}

variable "secret_key" {
  type        = string
  description = "Additional backend secret key."
  sensitive   = true
}

variable "jwt_expiration" {
  type        = number
  default     = 86400000
  description = "JWT expiration in milliseconds."
}

variable "openai_access_token" {
  type        = string
  default     = ""
  description = "OpenAI token injected into Elastic Beanstalk."
  sensitive   = true
}

variable "nanobanana_access_token" {
  type        = string
  default     = ""
  description = "NanoBanana token injected into Elastic Beanstalk."
  sensitive   = true
}
