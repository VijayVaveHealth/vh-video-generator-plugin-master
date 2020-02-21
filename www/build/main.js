webpackJsonp([0],{

/***/ 108:
/***/ (function(module, exports) {

function webpackEmptyAsyncContext(req) {
	// Here Promise.resolve().then() is used instead of new Promise() to prevent
	// uncatched exception popping up in devtools
	return Promise.resolve().then(function() {
		throw new Error("Cannot find module '" + req + "'.");
	});
}
webpackEmptyAsyncContext.keys = function() { return []; };
webpackEmptyAsyncContext.resolve = webpackEmptyAsyncContext;
module.exports = webpackEmptyAsyncContext;
webpackEmptyAsyncContext.id = 108;

/***/ }),

/***/ 149:
/***/ (function(module, exports) {

function webpackEmptyAsyncContext(req) {
	// Here Promise.resolve().then() is used instead of new Promise() to prevent
	// uncatched exception popping up in devtools
	return Promise.resolve().then(function() {
		throw new Error("Cannot find module '" + req + "'.");
	});
}
webpackEmptyAsyncContext.keys = function() { return []; };
webpackEmptyAsyncContext.resolve = webpackEmptyAsyncContext;
module.exports = webpackEmptyAsyncContext;
webpackEmptyAsyncContext.id = 149;

/***/ }),

/***/ 193:
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return HomePage; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1_ionic_angular__ = __webpack_require__(54);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};


var HomePage = /** @class */ (function () {
    function HomePage(navCtrl) {
        this.navCtrl = navCtrl;
        var self = this;
        window.onload = function () {
            self.drawOnCanvas("testImage01");
        };
    }
    HomePage.prototype.drawOnCanvas = function (imageId) {
        var c = document.getElementById("canvas");
        var ctx = c.getContext("2d");
        var img = document.getElementById(imageId);
        var size = 256;
        c.width = size;
        c.height = size;
        ctx.drawImage(img, 0, 0, size, size);
        this.imageData = ctx.getImageData(0, 0, size, size);
    };
    HomePage.prototype.classify = function (event) {
        var emtId = event.target.id;
        this.drawOnCanvas(emtId);
        cordova.plugins.FrameToVideoPlugin.start({ "timestamp": 0, "width": this.imageData.width, "height": this.imageData.height });
        this.run(1000);
    };
    HomePage.prototype.run = function (times) {
        var self = this;
        console.log(this.imageData.data.buffer);
        cordova.plugins.FrameToVideoPlugin.addFrame(this.imageData.data.buffer, { "type": 1, "timestamp": times }, function (msg) {
            if (times == 100000) {
                cordova.plugins.FrameToVideoPlugin.end(function (url) {
                    console.log(url["fileURL"]);
                    var path = url["fileURL"];
                    alert("Video saved to camera roll");
                });
            }
            else {
                self.run(times + 1000);
            }
        }, function (err) {
            alert(err);
        });
    };
    HomePage = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["m" /* Component */])({
            selector: 'page-home',template:/*ion-inline-start:"/Users/arsu/vh-video-generator-plugin/src/pages/home/home.html"*/'<ion-header>\n  <ion-navbar>\n    <ion-title>\n      Home\n    </ion-title>\n  </ion-navbar>\n</ion-header>\n\n<ion-content class="HomePage">\n  <ion-card>\n    <ion-card-content>\n      Hello World, this is my test app\n\n      Choose Picture:\n\n      <p>Class 0:</p>\n      <ion-row>\n        <ion-col col-4><img src="assets/imgs/0/0frame1.png" alt="your image" id="testImage01" (click)="classify($event)"></ion-col>\n        <ion-col col-4><img src="assets/imgs/0/0frame2.png" alt="your image" id="testImage02" (click)="classify($event)"></ion-col>\n        <ion-col col-4><img src="assets/imgs/0/0frame3.png" alt="your image" id="testImage03" (click)="classify($event)"></ion-col>\n      </ion-row>\n      <p>Class 1:</p>\n      <ion-row>\n        <ion-col col-6><img src="assets/imgs/1/1frame1.png" alt="your image" id="testImage11" (click)="classify($event)"></ion-col>\n        <ion-col col-6><img src="assets/imgs/1/1frame2.png" alt="your image" id="testImage12" (click)="classify($event)"></ion-col>\n      </ion-row>\n      <p>Class 2:</p>\n      <ion-row>\n        <ion-col col-4><img src="assets/imgs/2/2frame1.png" alt="your image" id="testImage21" (click)="classify($event)"></ion-col>\n        <ion-col col-4><img src="assets/imgs/2/2frame2.png" alt="your image" id="testImage22" (click)="classify($event)"></ion-col>\n        <ion-col col-4><img src="assets/imgs/2/2frame3.png" alt="your image" id="testImage23" (click)="classify($event)"></ion-col>\n        <ion-col col-6><img src="assets/imgs/2/2frame4.png" alt="your image" id="testImage24" (click)="classify($event)"></ion-col>\n        <ion-col col-6><img src="assets/imgs/2/2frame5.png" alt="your image" id="testImage25" (click)="classify($event)"></ion-col>\n      </ion-row>\n      <p>Class 3:</p>\n      <ion-row>\n        <ion-col col-6><img src="assets/imgs/3/3frame1.png" alt="your image" id="testImage31" (click)="classify($event)"></ion-col>\n        <ion-col col-6><img src="assets/imgs/3/3frame2.png" alt="your image" id="testImage32" (click)="classify($event)"></ion-col>\n      </ion-row>\n      <p>Class 4:</p>\n      <ion-row>\n        <ion-col col-4><img src="assets/imgs/4/4frame1.png" alt="your image" id="testImage41" (click)="classify($event)"></ion-col>\n        <ion-col col-4><img src="assets/imgs/4/4frame2.png" alt="your image" id="testImage42" (click)="classify($event)"></ion-col>\n        <ion-col col-4><img src="assets/imgs/4/4frame3.png" alt="your image" id="testImage43" (click)="classify($event)"></ion-col>\n      </ion-row>\n      <p>Class 5:</p>\n      <ion-row>\n        <ion-col col-4><img src="assets/imgs/5/5frame1.png" alt="your image" id="testImage51" (click)="classify($event)"></ion-col>\n        <ion-col col-4><img src="assets/imgs/5/5frame2.png" alt="your image" id="testImage52" (click)="classify($event)"></ion-col>\n        <ion-col col-4><img src="assets/imgs/5/5frame3.png" alt="your image" id="testImage53" (click)="classify($event)"></ion-col>\n        <ion-col col-6><img src="assets/imgs/5/5frame4.png" alt="your image" id="testImage54" (click)="classify($event)"></ion-col>\n        <ion-col col-6><img src="assets/imgs/5/5frame5.png" alt="your image" id="testImage55" (click)="classify($event)"></ion-col>\n      </ion-row>\n      <p>Class 6:</p>\n      <ion-row>\n        <ion-col col-4><img src="assets/imgs/6/6frame1.png" alt="your image" id="testImage61" (click)="classify($event)"></ion-col>\n        <ion-col col-4><img src="assets/imgs/6/6frame2.png" alt="your image" id="testImage62" (click)="classify($event)"></ion-col>\n        <ion-col col-4><img src="assets/imgs/6/6frame3.png" alt="your image" id="testImage63" (click)="classify($event)"></ion-col>\n      </ion-row>\n      <canvas id="canvas"></canvas>\n      <p *ngIf="logg">{{logg}}</p>\n    </ion-card-content>\n  </ion-card>\n</ion-content>\n'/*ion-inline-end:"/Users/arsu/vh-video-generator-plugin/src/pages/home/home.html"*/
        }),
        __metadata("design:paramtypes", [__WEBPACK_IMPORTED_MODULE_1_ionic_angular__["d" /* NavController */]])
    ], HomePage);
    return HomePage;
}());

//# sourceMappingURL=home.js.map

/***/ }),

/***/ 194:
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
Object.defineProperty(__webpack_exports__, "__esModule", { value: true });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_platform_browser_dynamic__ = __webpack_require__(195);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__app_module__ = __webpack_require__(217);


Object(__WEBPACK_IMPORTED_MODULE_0__angular_platform_browser_dynamic__["a" /* platformBrowserDynamic */])().bootstrapModule(__WEBPACK_IMPORTED_MODULE_1__app_module__["a" /* AppModule */]);
//# sourceMappingURL=main.js.map

/***/ }),

/***/ 217:
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return AppModule; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_platform_browser__ = __webpack_require__(30);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2_ionic_angular__ = __webpack_require__(54);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__ionic_native_splash_screen__ = __webpack_require__(189);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4__ionic_native_status_bar__ = __webpack_require__(192);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_5__app_component__ = __webpack_require__(267);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_6__pages_home_home__ = __webpack_require__(193);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};







var AppModule = /** @class */ (function () {
    function AppModule() {
    }
    AppModule = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_1__angular_core__["I" /* NgModule */])({
            declarations: [
                __WEBPACK_IMPORTED_MODULE_5__app_component__["a" /* MyApp */],
                __WEBPACK_IMPORTED_MODULE_6__pages_home_home__["a" /* HomePage */]
            ],
            imports: [
                __WEBPACK_IMPORTED_MODULE_0__angular_platform_browser__["a" /* BrowserModule */],
                __WEBPACK_IMPORTED_MODULE_2_ionic_angular__["c" /* IonicModule */].forRoot(__WEBPACK_IMPORTED_MODULE_5__app_component__["a" /* MyApp */], {}, {
                    links: []
                }),
            ],
            bootstrap: [__WEBPACK_IMPORTED_MODULE_2_ionic_angular__["a" /* IonicApp */]],
            entryComponents: [
                __WEBPACK_IMPORTED_MODULE_5__app_component__["a" /* MyApp */],
                __WEBPACK_IMPORTED_MODULE_6__pages_home_home__["a" /* HomePage */]
            ],
            providers: [
                __WEBPACK_IMPORTED_MODULE_4__ionic_native_status_bar__["a" /* StatusBar */],
                __WEBPACK_IMPORTED_MODULE_3__ionic_native_splash_screen__["a" /* SplashScreen */],
                { provide: __WEBPACK_IMPORTED_MODULE_1__angular_core__["u" /* ErrorHandler */], useClass: __WEBPACK_IMPORTED_MODULE_2_ionic_angular__["b" /* IonicErrorHandler */] }
            ]
        })
    ], AppModule);
    return AppModule;
}());

//# sourceMappingURL=app.module.js.map

/***/ }),

/***/ 267:
/***/ (function(module, __webpack_exports__, __webpack_require__) {

"use strict";
/* harmony export (binding) */ __webpack_require__.d(__webpack_exports__, "a", function() { return MyApp; });
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_0__angular_core__ = __webpack_require__(0);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_1_ionic_angular__ = __webpack_require__(54);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_2__ionic_native_status_bar__ = __webpack_require__(192);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_3__ionic_native_splash_screen__ = __webpack_require__(189);
/* harmony import */ var __WEBPACK_IMPORTED_MODULE_4__pages_home_home__ = __webpack_require__(193);
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};





var MyApp = /** @class */ (function () {
    function MyApp(platform, statusBar, splashScreen) {
        this.rootPage = __WEBPACK_IMPORTED_MODULE_4__pages_home_home__["a" /* HomePage */];
        platform.ready().then(function () {
            // Okay, so the platform is ready and our plugins are available.
            // Here you can do any higher level native things you might need.
            statusBar.styleDefault();
            splashScreen.hide();
        });
    }
    MyApp = __decorate([
        Object(__WEBPACK_IMPORTED_MODULE_0__angular_core__["m" /* Component */])({template:/*ion-inline-start:"/Users/arsu/vh-video-generator-plugin/src/app/app.html"*/'<ion-menu [content]="content">\n\n    <ion-header>\n      <ion-toolbar>\n        <ion-title>Pages</ion-title>\n      </ion-toolbar>\n    </ion-header>\n  \n    <ion-content>\n      <ion-list>\n        <button ion-item *ngFor="let p of pages" (click)="openPage(p)">\n          {{p.title}}\n        </button>\n      </ion-list>\n    </ion-content>\n  \n  </ion-menu>\n  \n  <ion-nav [root]="rootPage" #content swipeBackEnabled="false"></ion-nav>'/*ion-inline-end:"/Users/arsu/vh-video-generator-plugin/src/app/app.html"*/
        }),
        __metadata("design:paramtypes", [__WEBPACK_IMPORTED_MODULE_1_ionic_angular__["e" /* Platform */], __WEBPACK_IMPORTED_MODULE_2__ionic_native_status_bar__["a" /* StatusBar */], __WEBPACK_IMPORTED_MODULE_3__ionic_native_splash_screen__["a" /* SplashScreen */]])
    ], MyApp);
    return MyApp;
}());

//# sourceMappingURL=app.component.js.map

/***/ })

},[194]);
//# sourceMappingURL=main.js.map