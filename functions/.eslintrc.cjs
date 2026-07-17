module.exports = {
  root: true,
  env: {
    es2020: true,
    node: true,
    mocha: true,
  },
  parser: "@typescript-eslint/parser",
  parserOptions: {
    project: ["./tsconfig.json"],
    sourceType: "module",
  },
  plugins: ["@typescript-eslint"],
  extends: [
    "eslint:recommended",
    "plugin:@typescript-eslint/recommended",
  ],
  rules: {
    "quotes": ["error", "double"],
    "semi": ["error", "always"],
    "max-len": ["warn", { "code": 120 }],
    "object-curly-spacing": ["error", "always"],
    "@typescript-eslint/no-explicit-any": "off"
  },
};
