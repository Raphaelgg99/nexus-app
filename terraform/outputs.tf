output "frontend_bucket_name" {
  value       = aws_s3_bucket.frontend.bucket
  description = "S3 bucket that stores the built Angular frontend."
}

output "cloudfront_distribution_id" {
  value       = aws_cloudfront_distribution.frontend.id
  description = "CloudFront distribution ID for cache invalidation."
}

output "cloudfront_domain_name" {
  value       = aws_cloudfront_distribution.frontend.domain_name
  description = "CloudFront domain for the frontend."
}

output "elastic_beanstalk_application_name" {
  value       = aws_elastic_beanstalk_application.backend.name
  description = "Elastic Beanstalk application name used by the deploy workflow."
}

output "elastic_beanstalk_environment_name" {
  value       = aws_elastic_beanstalk_environment.backend.name
  description = "Elastic Beanstalk environment name used by the deploy workflow."
}

output "elastic_beanstalk_artifact_bucket" {
  value       = aws_s3_bucket.eb_artifacts.bucket
  description = "S3 bucket where GitHub Actions uploads backend deploy bundles."
}

output "elastic_beanstalk_environment_url" {
  value       = aws_elastic_beanstalk_environment.backend.cname
  description = "Elastic Beanstalk environment CNAME."
}

output "database_endpoint" {
  value       = aws_db_instance.postgres.address
  description = "PostgreSQL endpoint."
}
