const path = require("path");

const config = {
  entry: path.join(__dirname, "src", "main", "js", "index.jsx"),
  output: {
    path: path.join(__dirname, "src", "main", "resources", "public", "built"),
    filename: "bundle.js",
    chunkFilename: "[name].js",
  },
  module: {
    rules: [
      {
        test: /.jsx?$/,
        exclude: [path.resolve(__dirname, "node_modules")],
        loader: "babel-loader",
        options: {
          presets: ["@babel/preset-env", "@babel/preset-react"],
        },
      },
    ],
  },
  resolve: {
    extensions: [".json", ".js", ".jsx"],
  },
};

module.exports = (env, argv) => {
  const devMode = argv.mode !== "production";

  if (devMode) {
    config.devtool = "source-map";
  }

  config.module.rules.push({
    test: /\.css$/,
    use: ["style-loader", "css-loader"],
  });

  return config;
};
