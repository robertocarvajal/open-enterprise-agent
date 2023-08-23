{
  inputs,
  cell,
}: let
  inherit (inputs) nixpkgs std cells;
in {
  default = std.lib.dev.mkShell {
    commands = [
      {
        package = std.packages.default;
        category = "nix";
      }
      {
        package = nixpkgs.jq;
      }
      {
        package = nixpkgs.teleport;
      }
      {
        package = nixpkgs.awscli;
        category = "aws";
      }
      # {
      #   package = cells.vendor.packages.aws-mfa-tools;
      #   category = "aws";
      # }
      {
        package = nixpkgs.terraform;
        category = "terraform";
      }
      {
        package = nixpkgs.terragrunt;
        category = "terraform";
      }
      {
        package = nixpkgs.k9s;
        category = "kubernetes";
      }
      {
        package = nixpkgs.kubernetes-helm;
        category = "kubernetes";
      }
      {
        package = nixpkgs.kubectl;
        category = "kubernetes";
      }
      {
        package = nixpkgs.kustomize;
        category = "kubernetes";
      }
    ];
  };
}
