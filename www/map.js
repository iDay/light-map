var cordova = require('cordova'),
    exec = require('cordova/exec');

var baidumap = baidumap || {};

/**
 * 初始化
 * @param{Object} options 配置项
 * @param{Function} callback 回调
 */
baidumap.init = function(options, callback) {
	exec(callback, function() {
	}, 'BaiduMap', 'init', [options]);
};

baidumap.setCenter = function(options, callback) {
	exec(callback, function() {
	}, 'BaiduMap', 'setCenter', [options]);
};

baidumap.setZoom = function(options, callback) {
	exec(callback, function() {
	}, 'BaiduMap', 'setZoom', [options]);
};

module.exports = baidumap;