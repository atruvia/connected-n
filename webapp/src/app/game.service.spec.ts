import { TestBed } from '@angular/core/testing';
import { HttpClientModule } from '@angular/common/http';
import { GameService } from './game.service';
import { PactWeb, Matchers } from '@pact-foundation/pact-web';
import { Game } from './Game';

describe('GameService', () => {

  let provider;

  beforeAll( function(done) {

    provider = new PactWeb({
      consumer: 'ui',
      provider: 'gamesservice',
      port: 1234,
      host: '127.0.0.1',
    });

    // required for slower CI environments
    setTimeout(done, 2000);

    // Required if run with `singleRun: false`
    provider.removeInteractions();
  });

  afterAll(function (done) {
    provider.finalize()
    .then(function () {
      done();
    }, function (err) {
      done.fail(err);
    });
  });

  beforeEach(() => TestBed.configureTestingModule({
    imports: [
      HttpClientModule
    ],
    providers: [
      GameService
    ],
  }));

  afterEach((done) => {
    provider.verify().then(done, e => done.fail(e));
  });

  it('should be created', () => {
    const service: GameService = TestBed.get(GameService);
    expect(service).toBeTruthy();
  });

  describe('list games', () => {

    const expectedGames: Game[] = [];

    beforeAll((done) => {

      provider.addInteraction({
        state: 'provider lists all games',
        uponReceiving: 'a request to GET all games',
        withRequest: {
          method: 'GET',
          path: '/api/games',
          headers: {
            'Content-Type': 'application/json'
          }
        },
        willRespondWith: {
          status: 200,
          headers: {
            'Content-Type': 'application/json'
          },
          body: Matchers.somethingLike(
            expectedGames
          )
        }
      }).then(done, error => done.fail(error));
    });

    // it('should list games', (done) => {
    //   const games: GameService = TestBed.get(GameService);
    //   games.getAll().subscribe( (response: Game[]) => {
    //     expect(response).toEqual(expectedGames);

    //     done();
    //   }, error => {
    //     done.fail(error);
    //   }
    //   )
    // })
  });
});
